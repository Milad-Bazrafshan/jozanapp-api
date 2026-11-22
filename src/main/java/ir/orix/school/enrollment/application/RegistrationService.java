package ir.orix.school.enrollment.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.academic.domain.ProgramCourse;
import ir.orix.school.academic.persistence.ProgramCourseRepository;
import ir.orix.school.classroom.domain.ClassSection;
import ir.orix.school.classroom.domain.CourseOffering;
import ir.orix.school.classroom.persistence.ClassSectionRepository;
import ir.orix.school.enrollment.domain.Enrollment;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.people.application.PeopleService;
import ir.orix.school.shared.web.ApiException;

@Service
public class RegistrationService {
    private final TenantRepository tenants;
    private final ClassSectionRepository sections;
    private final EnrollmentService enrollments;
    private final ProgramCourseRepository programCourses;
    private final PeopleService people;
    private final Clock clock;
    private final ir.orix.school.enrollment.persistence.EnrollmentRepository enrollmentRows;

    public RegistrationService(TenantRepository tenants, ClassSectionRepository sections, EnrollmentService enrollments,
            ProgramCourseRepository programCourses, PeopleService people, Clock clock,
            ir.orix.school.enrollment.persistence.EnrollmentRepository enrollmentRows) {
        this.tenants = tenants; this.sections = sections; this.enrollments = enrollments; this.programCourses = programCourses;
        this.people = people; this.clock = clock; this.enrollmentRows = enrollmentRows;
    }

    @Transactional(readOnly = true)
    public List<OfferingView> catalog(Long tenantId) {
        requireEnabled(tenantId);
        LocalDate today = LocalDate.now(clock);
        return sections.findAllByTenantIdOrderByName(tenantId).stream()
                .filter(section -> section.getStatus() == ClassSection.Status.ACTIVE && section.getOffering().getStatus() == CourseOffering.Status.OPEN)
                .filter(section -> insideWindow(section.getOffering(), today))
                .map(section -> new OfferingView(section.getId(), section.getName(), section.getOffering().getCourse().getTitle(),
                        section.getOffering().getCourse().getId(), section.getCapacity(), enrollmentRows.countActive(section.getId()),
                        section.getOffering().getEnrollmentStartsOn(), section.getOffering().getEnrollmentEndsOn()))
                .toList();
    }

    @Transactional
    public EnrollmentService.View register(Long tenantId, Long membershipId, Long actorId, Long studentId, Long classSectionId, boolean waitlist) {
        requireEnabled(tenantId);
        PeopleService.MyProfiles profiles = people.myProfiles(tenantId, membershipId);
        if (profiles.student() != null && profiles.student().id().equals(studentId)) {
            // self
        } else {
            people.requireChildAcademicAccess(tenantId, membershipId, studentId);
        }
        ClassSection section = sections.findByIdAndTenantId(classSectionId, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLASS_SECTION_NOT_FOUND", "Class was not found."));
        ensurePrerequisites(tenantId, studentId, section);
        return enrollments.enroll(tenantId, actorId, new EnrollmentService.Command(studentId, classSectionId, LocalDate.now(clock), waitlist, "self-service"));
    }

    private void ensurePrerequisites(Long tenantId, Long studentId, ClassSection section) {
        List<ProgramCourse> links = programCourses.findAllByTenantIdOrderByProgramNameAscSequenceNumberAsc(tenantId).stream()
                .filter(item -> item.getCourse().getId().equals(section.getOffering().getCourse().getId()) && item.isRequired())
                .toList();
        if (links.isEmpty()) return;
        java.util.Set<Long> taken = enrollmentRows.findAllByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantId, studentId).stream()
                .filter(item -> item.getStatus() == Enrollment.Status.ACTIVE || item.getStatus() == Enrollment.Status.COMPLETED)
                .map(item -> item.getClassSection().getOffering().getCourse().getId()).collect(java.util.stream.Collectors.toSet());
        for (ProgramCourse link : links) {
            if (link.getSequenceNumber() != null && link.getSequenceNumber() > 1) {
                boolean prior = programCourses.findAllByTenantIdOrderByProgramNameAscSequenceNumberAsc(tenantId).stream()
                        .filter(item -> item.getProgram().getId().equals(link.getProgram().getId()) && item.getSequenceNumber() != null && item.getSequenceNumber() < link.getSequenceNumber())
                        .allMatch(item -> taken.contains(item.getCourse().getId()) || item.getCourse().getId().equals(section.getOffering().getCourse().getId()));
                if (!prior) throw new ApiException(HttpStatus.CONFLICT, "PREREQUISITE_MISSING", "Required prior courses are not completed.");
            }
        }
    }

    private boolean insideWindow(CourseOffering offering, LocalDate today) {
        if (offering.getEnrollmentStartsOn() != null && today.isBefore(offering.getEnrollmentStartsOn())) return false;
        if (offering.getEnrollmentEndsOn() != null && today.isAfter(offering.getEnrollmentEndsOn())) return false;
        return true;
    }

    private void requireEnabled(Long tenantId) {
        Tenant tenant = tenants.findById(tenantId).orElseThrow();
        if (!tenant.isCourseSelectionEnabled()) throw new ApiException(HttpStatus.NOT_FOUND, "REGISTRATION_DISABLED", "Course selection is disabled.");
    }

    public record OfferingView(Long classSectionId, String className, String courseTitle, Long courseId, int capacity, long occupied,
            LocalDate enrollmentStartsOn, LocalDate enrollmentEndsOn) {}
}
