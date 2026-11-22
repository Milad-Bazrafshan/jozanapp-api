package ir.orix.school.classroom.application;

import java.time.LocalDate;
import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.academic.domain.*;
import ir.orix.school.academic.persistence.*;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.classroom.domain.*;
import ir.orix.school.classroom.persistence.*;
import ir.orix.school.organization.domain.*;
import ir.orix.school.organization.persistence.*;
import ir.orix.school.people.domain.InstructorProfile;
import ir.orix.school.people.persistence.InstructorProfileRepository;
import ir.orix.school.shared.web.ApiException;

@Service
public class ClassroomService {
    private final TenantRepository tenants;
    private final AcademicPeriodRepository periods;
    private final CourseRepository courses;
    private final BranchRepository branches;
    private final CourseOfferingRepository offerings;
    private final ClassSectionRepository sections;
    private final InstructorProfileRepository instructors;
    private final ClassInstructorRepository assignments;
    private final AuditService audit;

    public ClassroomService(TenantRepository tenants, AcademicPeriodRepository periods, CourseRepository courses, BranchRepository branches, CourseOfferingRepository offerings, ClassSectionRepository sections, InstructorProfileRepository instructors, ClassInstructorRepository assignments, AuditService audit) {
        this.tenants = tenants;
        this.periods = periods;
        this.courses = courses;
        this.branches = branches;
        this.offerings = offerings;
        this.sections = sections;
        this.instructors = instructors;
        this.assignments = assignments;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Catalog catalog(Long tenantId) {
        return new Catalog(offerings.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(ClassroomService::offeringView).toList(), sections.findAllByTenantIdOrderByName(tenantId).stream().map(ClassroomService::sectionView).toList(), assignments.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(ClassroomService::instructorView).toList());
    }

    @Transactional
    public OfferingView createOffering(Long tenantId, Long actorId, OfferingCommand c) {
        if (c.enrollmentStartsOn() != null && c.enrollmentEndsOn() != null && c.enrollmentEndsOn().isBefore(c.enrollmentStartsOn()))
            throw invalid("INVALID_ENROLLMENT_DATES", "Enrollment end date cannot be before its start date.");
        if (offerings.existsByTenantIdAndCodeIgnoreCase(tenantId, c.code())) throw conflict("OFFERING_CODE_EXISTS");
        Tenant tenant = tenant(tenantId);
        AcademicPeriod period = periods.findByIdAndTenantId(c.academicPeriodId(), tenantId).orElseThrow(notFound("ACADEMIC_PERIOD_NOT_FOUND"));
        Course course = courses.findByIdAndTenantId(c.courseId(), tenantId).orElseThrow(notFound("COURSE_NOT_FOUND"));
        Branch branch = branches.findByIdAndTenantId(c.branchId(), tenantId).orElseThrow(notFound("BRANCH_NOT_FOUND"));
        CourseOffering value = offerings.save(new CourseOffering(tenant, period, course, branch, c.code(), c.enrollmentStartsOn(), c.enrollmentEndsOn()));
        audit.record(tenantId, actorId, "COURSE_OFFERING_CREATED", "COURSE_OFFERING", value.getId(), Map.of("code", value.getCode()));
        return offeringView(value);
    }

    @Transactional
    public SectionView createSection(Long tenantId, Long actorId, SectionCommand c) {
        CourseOffering offering = offerings.findByIdAndTenantId(c.offeringId(), tenantId).orElseThrow(notFound("OFFERING_NOT_FOUND"));
        if (sections.existsByOfferingIdAndCodeIgnoreCase(offering.getId(), c.code()))
            throw conflict("CLASS_CODE_EXISTS");
        ClassSection value = sections.save(new ClassSection(tenant(tenantId), offering, c.code(), c.name(), c.capacity(), c.deliveryMode()));
        audit.record(tenantId, actorId, "CLASS_SECTION_CREATED", "CLASS_SECTION", value.getId(), Map.of("code", value.getCode()));
        return sectionView(value);
    }

    @Transactional
    public InstructorView assignInstructor(Long tenantId, Long actorId, InstructorCommand c) {
        if (c.effectiveTo() != null && c.effectiveTo().isBefore(c.effectiveFrom()))
            throw invalid("INVALID_ASSIGNMENT_DATES", "Assignment end date cannot be before its start date.");
        ClassSection section = sections.findByIdAndTenantId(c.classSectionId(), tenantId).orElseThrow(notFound("CLASS_SECTION_NOT_FOUND"));
        InstructorProfile instructor = instructors.findByIdAndTenantId(c.instructorId(), tenantId).orElseThrow(notFound("INSTRUCTOR_NOT_FOUND"));
        if (assignments.existsByClassSectionIdAndInstructorIdAndStatus(section.getId(), instructor.getId(), ClassInstructor.Status.ACTIVE))
            throw conflict("INSTRUCTOR_ALREADY_ASSIGNED");
        ClassInstructor value = assignments.save(new ClassInstructor(tenant(tenantId), section, instructor, c.role(), c.primary(), c.effectiveFrom(), c.effectiveTo()));
        audit.record(tenantId, actorId, "INSTRUCTOR_ASSIGNED", "CLASS_SECTION", section.getId(), Map.of("instructorId", instructor.getId().toString()));
        return instructorView(value);
    }

    private Tenant tenant(Long id) {
        return tenants.findById(id).orElseThrow(notFound("TENANT_NOT_FOUND"));
    }

    private static ApiException conflict(String code) {
        return new ApiException(HttpStatus.CONFLICT, code, "The value or assignment already exists.");
    }

    private static ApiException invalid(String code, String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, detail);
    }

    private static java.util.function.Supplier<ApiException> notFound(String code) {
        return () -> new ApiException(HttpStatus.NOT_FOUND, code, "The requested classroom item was not found.");
    }

    private static OfferingView offeringView(CourseOffering o) {
        return new OfferingView(o.getId(), o.getCode(), o.getCourse().getId(), o.getCourse().getTitle(), o.getAcademicPeriod().getId(), o.getAcademicPeriod().getName(), o.getBranch().getId(), o.getBranch().getName(), o.getEnrollmentStartsOn(), o.getEnrollmentEndsOn(), o.getStatus().name());
    }

    private static SectionView sectionView(ClassSection s) {
        return new SectionView(s.getId(), s.getOffering().getId(), s.getOffering().getCourse().getTitle(), s.getOffering().getBranch().getName(), s.getCode(), s.getName(), s.getCapacity(), s.getDeliveryMode().name(), s.getStatus().name());
    }

    private static InstructorView instructorView(ClassInstructor a) {
        return new InstructorView(a.getId(), a.getClassSection().getId(), a.getClassSection().getName(), a.getInstructor().getId(), a.getInstructor().getPerson().getFullName(), a.getRole().name(), a.isPrimary(), a.getEffectiveFrom(), a.getEffectiveTo(), a.getStatus().name());
    }

    public record OfferingCommand(Long academicPeriodId, Long courseId, Long branchId, String code,
                                  LocalDate enrollmentStartsOn, LocalDate enrollmentEndsOn) {
    }

    public record SectionCommand(Long offeringId, String code, String name, int capacity,
                                 ClassSection.DeliveryMode deliveryMode) {
    }

    public record InstructorCommand(Long classSectionId, Long instructorId, ClassInstructor.Role role, boolean primary,
                                    LocalDate effectiveFrom, LocalDate effectiveTo) {
    }

    public record OfferingView(Long id, String code, Long courseId, String courseTitle, Long academicPeriodId,
                               String academicPeriodName, Long branchId, String branchName,
                               LocalDate enrollmentStartsOn, LocalDate enrollmentEndsOn, String status) {
    }

    public record SectionView(Long id, Long offeringId, String courseTitle, String branchName, String code, String name,
                              int capacity, String deliveryMode, String status) {
    }

    public record InstructorView(Long id, Long classSectionId, String className, Long instructorId,
                                 String instructorName, String role, boolean primary, LocalDate effectiveFrom,
                                 LocalDate effectiveTo, String status) {
    }

    public record Catalog(List<OfferingView> offerings, List<SectionView> sections, List<InstructorView> instructors) {
    }
}
