package ir.orix.school.people.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.identity.persistence.UserAccountRepository;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.organization.persistence.TenantMembershipRepository;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.people.domain.*;
import ir.orix.school.people.persistence.*;
import ir.orix.school.kyc.NationalIdentityResult;
import ir.orix.school.kyc.ZohalThirdService;
import ir.orix.school.shared.web.ApiException;

@Service
public class PeopleService {
    private final TenantRepository tenants;
    private final PersonRepository people;
    private final StudentProfileRepository students;
    private final InstructorProfileRepository instructors;
    private final GuardianProfileRepository guardians;
    private final StudentGuardianRepository relationships;
    private final UserAccountRepository accounts;
    private final TenantMembershipRepository memberships;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final Clock clock;
    private final ZohalThirdService zohal;

    public PeopleService(TenantRepository tenants, PersonRepository people, StudentProfileRepository students,
                         InstructorProfileRepository instructors, GuardianProfileRepository guardians, StudentGuardianRepository relationships,
                         UserAccountRepository accounts, TenantMembershipRepository memberships, PasswordEncoder passwordEncoder,
                         AuditService audit, Clock clock, ZohalThirdService zohal) {
        this.tenants = tenants;
        this.people = people;
        this.students = students;
        this.instructors = instructors;
        this.guardians = guardians;
        this.relationships = relationships;
        this.accounts = accounts;
        this.memberships = memberships;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.clock = clock;
        this.zohal = zohal;
    }

    @Transactional(readOnly = true)
    public Directory directory(Long tenantId) {
        List<StudentView> studentViews = students.findAllByTenantIdOrderByPersonLastNameAsc(tenantId).stream().map(PeopleService::studentView).toList();
        List<InstructorView> instructorViews = instructors.findAllByTenantIdOrderByPersonLastNameAsc(tenantId).stream().map(PeopleService::instructorView).toList();
        List<GuardianView> guardianViews = guardians.findAllByTenantIdOrderByPersonLastNameAsc(tenantId).stream().map(PeopleService::guardianView).toList();
        List<RelationshipView> relationViews = relationships.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(PeopleService::relationshipView).toList();
        return new Directory(studentViews, instructorViews, guardianViews, relationViews);
    }

    @Transactional(readOnly = true)
    public MyProfiles myProfiles(Long tenantId, Long membershipId) {
        TenantMembership membership = memberships.findByIdAndTenantId(membershipId, tenantId).orElseThrow(notFound("MEMBERSHIP_NOT_FOUND"));
        Long personId = membership.getPerson().getId();
        StudentView student = students.findByTenantIdAndPersonId(tenantId, personId).map(PeopleService::studentView).orElse(null);
        InstructorView instructor = instructors.findByTenantIdAndPersonId(tenantId, personId).map(PeopleService::instructorView).orElse(null);
        GuardianProfile guardian = guardians.findByTenantIdAndPersonId(tenantId, personId).orElse(null);
        List<StudentView> children = guardian == null ? List.of() : relationships.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream()
            .filter(link -> link.getGuardian().getId().equals(guardian.getId()) && link.getEffectiveTo() == null && link.hasAcademicAccess())
            .map(StudentGuardian::getStudent).map(PeopleService::studentView).toList();
        return new MyProfiles(student, instructor, guardian == null ? null : guardianView(guardian), children);
    }

    @Transactional(readOnly = true)
    public void requireChildAcademicAccess(Long tenantId, Long membershipId, Long studentId) {
        MyProfiles profiles = myProfiles(tenantId, membershipId);
        if (profiles.guardian() == null) throw forbidden("GUARDIAN_PROFILE_REQUIRED");
        boolean allowed = profiles.children().stream().anyMatch(child -> child.id().equals(studentId));
        if (!allowed) throw forbidden("CHILD_ACCESS_DENIED");
    }

    @Transactional(readOnly = true)
    public void requireChildFinancialAccess(Long tenantId, Long membershipId, Long studentId) {
        TenantMembership membership = memberships.findByIdAndTenantId(membershipId, tenantId).orElseThrow(notFound("MEMBERSHIP_NOT_FOUND"));
        GuardianProfile guardian = guardians.findByTenantIdAndPersonId(tenantId, membership.getPerson().getId()).orElseThrow(() -> forbidden("GUARDIAN_PROFILE_REQUIRED"));
        boolean allowed = relationships.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream()
            .anyMatch(link -> link.getGuardian().getId().equals(guardian.getId())
                && link.getStudent().getId().equals(studentId)
                && link.getEffectiveTo() == null
                && link.hasFinancialAccess());
        if (!allowed)
            throw new ApiException(HttpStatus.FORBIDDEN, "CHILD_FINANCIAL_ACCESS_DENIED", "You do not have financial access to this student.");
    }

    @Transactional
    public StudentView createStudent(Long tenantId, Long actorId, CreateStudent command) {
        requireUniqueCode(!students.existsByTenantIdAndStudentCodeIgnoreCase(tenantId, command.code()), "STUDENT_CODE_EXISTS");
        Tenant tenant = tenant(tenantId);
        Person person = createPerson(tenant, command.person());
        StudentProfile profile = students.save(new StudentProfile(tenant, person, command.code(), command.admissionDate()));
        createOptionalLogin(tenant, person, command.person().email(), command.person().initialPassword());
        audit.record(tenantId, actorId, "STUDENT_CREATED", "STUDENT_PROFILE", profile.getId(), Map.of("code", profile.getStudentCode()));
        return studentView(profile);
    }

    /**
     * Student registration using Zohal civil registry (national code + birth date).
     */
    @Transactional
    public StudentView createStudentFromNationalRegistry(Long tenantId, Long actorId, CreateStudentFromRegistry command) {
        requireUniqueCode(!students.existsByTenantIdAndStudentCodeIgnoreCase(tenantId, command.code()), "STUDENT_CODE_EXISTS");
        NationalIdentityResult identity = zohal.nationalIdentityInquiry(command.nationalCode(), command.birthDate(), command.gender());
        Tenant tenant = tenant(tenantId);
        Person person = createPersonFromIdentity(tenant, identity, command.mobile());
        StudentProfile profile = students.save(new StudentProfile(tenant, person, command.code(), command.admissionDate()));
        createOptionalLogin(tenant, person, command.email(), command.initialPassword());
        audit.record(tenantId, actorId, "STUDENT_CREATED", "STUDENT_PROFILE", profile.getId(), Map.of("code", profile.getStudentCode(), "source", "ZOHAL_IDENTITY"));
        return studentView(profile);
    }

    @Transactional(readOnly = true)
    public NationalIdentityResult previewNationalIdentity(String nationalCode, String birthDate, Boolean gender) {
        return zohal.nationalIdentityInquiry(nationalCode, birthDate, gender);
    }

    @Transactional
    public InstructorView createInstructor(Long tenantId, Long actorId, CreateInstructor command) {
        requireUniqueCode(!instructors.existsByTenantIdAndInstructorCodeIgnoreCase(tenantId, command.code()), "INSTRUCTOR_CODE_EXISTS");
        Tenant tenant = tenant(tenantId);
        Person person = createPerson(tenant, command.person());
        InstructorProfile profile = instructors.save(new InstructorProfile(tenant, person, command.code(), command.title(), command.specialization()));
        createOptionalLogin(tenant, person, command.person().email(), command.person().initialPassword());
        audit.record(tenantId, actorId, "INSTRUCTOR_CREATED", "INSTRUCTOR_PROFILE", profile.getId(), Map.of("code", profile.getInstructorCode()));
        return instructorView(profile);
    }

    @Transactional
    public InstructorView createInstructorFromNationalRegistry(Long tenantId, Long actorId, CreateInstructorFromRegistry command) {
        requireUniqueCode(!instructors.existsByTenantIdAndInstructorCodeIgnoreCase(tenantId, command.code()), "INSTRUCTOR_CODE_EXISTS");
        NationalIdentityResult identity = zohal.nationalIdentityInquiry(command.nationalCode(), command.birthDate(), command.gender());
        Tenant tenant = tenant(tenantId);
        Person person = createPersonFromIdentity(tenant, identity, command.mobile());
        InstructorProfile profile = instructors.save(new InstructorProfile(tenant, person, command.code(), command.title(), command.specialization()));
        createOptionalLogin(tenant, person, command.email(), command.initialPassword());
        audit.record(tenantId, actorId, "INSTRUCTOR_CREATED", "INSTRUCTOR_PROFILE", profile.getId(), Map.of("code", profile.getInstructorCode(), "source", "ZOHAL_IDENTITY"));
        return instructorView(profile);
    }

    @Transactional
    public GuardianView createGuardian(Long tenantId, Long actorId, CreateGuardian command) {
        Tenant tenant = tenant(tenantId);
        Person person = createPerson(tenant, command.person());
        GuardianProfile profile = guardians.save(new GuardianProfile(tenant, person, command.occupation()));
        createOptionalLogin(tenant, person, command.person().email(), command.person().initialPassword());
        audit.record(tenantId, actorId, "GUARDIAN_CREATED", "GUARDIAN_PROFILE", profile.getId(), Map.of());
        return guardianView(profile);
    }

    @Transactional
    public RelationshipView linkGuardian(Long tenantId, Long actorId, LinkGuardian command) {
        StudentProfile student = students.findByIdAndTenantId(command.studentId(), tenantId).orElseThrow(notFound("STUDENT_NOT_FOUND"));
        GuardianProfile guardian = guardians.findByIdAndTenantId(command.guardianId(), tenantId).orElseThrow(notFound("GUARDIAN_NOT_FOUND"));
        StudentGuardian link = relationships.save(new StudentGuardian(tenant(tenantId), student, guardian, command.relationshipType(), command.primary(), command.academicAccess(), command.financialAccess(), command.pickupPermission(), command.receivesNotifications(), command.effectiveFrom()));
        audit.record(tenantId, actorId, "GUARDIAN_LINKED", "STUDENT_PROFILE", student.getId(), Map.of("guardianId", guardian.getId().toString()));
        return relationshipView(link);
    }

    private Person createPerson(Tenant tenant, PersonInput input) {
        if (input.nationalId() != null && !input.nationalId().isBlank() && people.existsByTenantIdAndNationalId(tenant.getId(), input.nationalId().strip()))
            throw conflict("NATIONAL_ID_EXISTS");
        return people.save(new Person(tenant, input.firstName(), input.lastName(), input.nationalId(), input.mobile()));
    }

    private Person createPersonFromIdentity(Tenant tenant, NationalIdentityResult identity, String mobile) {
        if (people.existsByTenantIdAndNationalId(tenant.getId(), identity.nationalCode()))
            throw conflict("NATIONAL_ID_EXISTS");
        return people.save(new Person(tenant, identity.firstName(), identity.lastName(), identity.nationalCode(), mobile,
            null, identity.fatherName(), null));
    }

    private void createOptionalLogin(Tenant tenant, Person person, String email, String password) {
        if (email == null || email.isBlank()) return;
        if (password == null || password.length() < 10)
            throw new ApiException(HttpStatus.BAD_REQUEST, "INITIAL_PASSWORD_REQUIRED", "An initial password of at least 10 characters is required when email is provided.");
        if (accounts.findByEmailIgnoreCase(email).isPresent()) throw conflict("ACCOUNT_EMAIL_EXISTS");
        UserAccount account = accounts.save(new UserAccount(email, passwordEncoder.encode(password), clock.instant()));
        memberships.save(new TenantMembership(tenant, account, person, clock.instant()));
    }

    private Tenant tenant(Long id) {
        return tenants.findById(id).orElseThrow(notFound("TENANT_NOT_FOUND"));
    }

    private static void requireUniqueCode(boolean valid, String code) {
        if (!valid) throw conflict(code);
    }

    private static ApiException conflict(String code) {
        return new ApiException(HttpStatus.CONFLICT, code, "The value already exists in this tenant.");
    }

    private static java.util.function.Supplier<ApiException> notFound(String code) {
        return () -> new ApiException(HttpStatus.NOT_FOUND, code, "The requested profile was not found.");
    }

    private static ApiException forbidden(String code) {
        return new ApiException(HttpStatus.FORBIDDEN, code, "You do not have academic access to this student.");
    }

    private static PersonView personView(Person p) {
        String dob = p.getDateOfBirth() == null ? null : p.getDateOfBirth().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return new PersonView(p.getId(), p.getFirstName(), p.getLastName(), p.getNationalId(), p.getMobile(), dob,
            p.getFathersName(), p.getPlaceOfBirth());
    }

    private static StudentView studentView(StudentProfile p) {
        return new StudentView(p.getId(), p.getStudentCode(), p.getStatus().name(), p.getAdmissionDate(), personView(p.getPerson()));
    }

    private static InstructorView instructorView(InstructorProfile p) {
        return new InstructorView(p.getId(), p.getInstructorCode(), p.getTitle(), p.getSpecialization(), p.getStatus().name(), personView(p.getPerson()));
    }

    private static GuardianView guardianView(GuardianProfile p) {
        return new GuardianView(p.getId(), p.getOccupation(), p.getStatus().name(), personView(p.getPerson()));
    }

    private static RelationshipView relationshipView(StudentGuardian r) {
        return new RelationshipView(r.getId(), studentView(r.getStudent()), guardianView(r.getGuardian()), r.getRelationshipType().name(), r.isPrimary(), r.hasAcademicAccess(), r.hasFinancialAccess(), r.hasPickupPermission(), r.isReceivesNotifications(), r.getEffectiveFrom(), r.getEffectiveTo());
    }

    public record PersonInput(String firstName, String lastName, String nationalId, String mobile, String email,
                              String initialPassword) {
    }

    public record CreateStudent(String code, LocalDate admissionDate, PersonInput person) {
    }

    public record CreateStudentFromRegistry(String code, LocalDate admissionDate,
                                            @jakarta.validation.constraints.NotBlank String nationalCode,
                                            @jakarta.validation.constraints.NotBlank String birthDate, Boolean gender,
                                            String mobile, @jakarta.validation.constraints.Email String email,
                                            String initialPassword) {
    }

    public record CreateInstructorFromRegistry(String code, String title, String specialization,
                                               @jakarta.validation.constraints.NotBlank String nationalCode,
                                               @jakarta.validation.constraints.NotBlank String birthDate,
                                               Boolean gender, String mobile,
                                               @jakarta.validation.constraints.Email String email,
                                               String initialPassword) {
    }

    public record CreateInstructor(String code, String title, String specialization, PersonInput person) {
    }

    public record CreateGuardian(String occupation, PersonInput person) {
    }

    public record LinkGuardian(Long studentId, Long guardianId, StudentGuardian.RelationshipType relationshipType,
                               boolean primary, boolean academicAccess, boolean financialAccess,
                               boolean pickupPermission, boolean receivesNotifications, LocalDate effectiveFrom) {
    }

    public record PersonView(Long id, String firstName, String lastName, String nationalId, String mobile,
                             String dateOfBirth, String fathersName, String placeOfBirth) {
    }

    public record StudentView(Long id, String code, String status, LocalDate admissionDate, PersonView person) {
    }

    public record InstructorView(Long id, String code, String title, String specialization, String status,
                                 PersonView person) {
    }

    public record GuardianView(Long id, String occupation, String status, PersonView person) {
    }

    public record RelationshipView(Long id, StudentView student, GuardianView guardian, String relationshipType,
                                   boolean primary, boolean academicAccess, boolean financialAccess,
                                   boolean pickupPermission, boolean receivesNotifications, LocalDate effectiveFrom,
                                   LocalDate effectiveTo) {
    }

    public record Directory(List<StudentView> students, List<InstructorView> instructors, List<GuardianView> guardians,
                            List<RelationshipView> relationships) {
    }

    public record MyProfiles(StudentView student, InstructorView instructor, GuardianView guardian,
                             List<StudentView> children) {
    }
}
