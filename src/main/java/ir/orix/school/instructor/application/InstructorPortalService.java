package ir.orix.school.instructor.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.assessment.application.AssessmentExecutionService;
import ir.orix.school.assessment.application.AssessmentService;
import ir.orix.school.assessment.persistence.AssessmentTargetRepository;
import ir.orix.school.attendance.application.AttendanceService;
import ir.orix.school.classroom.domain.ClassInstructor;
import ir.orix.school.classroom.domain.ClassSection;
import ir.orix.school.classroom.persistence.ClassInstructorRepository;
import ir.orix.school.enrollment.application.EnrollmentService;
import ir.orix.school.enrollment.domain.Enrollment;
import ir.orix.school.enrollment.persistence.EnrollmentRepository;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.organization.persistence.TenantMembershipRepository;
import ir.orix.school.people.domain.InstructorProfile;
import ir.orix.school.people.persistence.InstructorProfileRepository;
import ir.orix.school.scheduling.application.SchedulingService;
import ir.orix.school.scheduling.domain.ClassSession;
import ir.orix.school.scheduling.persistence.ClassSessionRepository;
import ir.orix.school.shared.web.ApiException;

@Service
public class InstructorPortalService {
    private final TenantMembershipRepository memberships;
    private final InstructorProfileRepository instructors;
    private final ClassInstructorRepository assignments;
    private final ClassSessionRepository sessions;
    private final EnrollmentRepository enrollments;
    private final AssessmentTargetRepository targets;
    private final AttendanceService attendance;
    private final SchedulingService scheduling;
    private final AssessmentService assessments;
    private final AssessmentExecutionService execution;
    private final Clock clock;

    public InstructorPortalService(TenantMembershipRepository memberships, InstructorProfileRepository instructors,
            ClassInstructorRepository assignments, ClassSessionRepository sessions, EnrollmentRepository enrollments,
            AssessmentTargetRepository targets, AttendanceService attendance, SchedulingService scheduling,
            AssessmentService assessments, AssessmentExecutionService execution, Clock clock) {
        this.memberships = memberships;
        this.instructors = instructors;
        this.assignments = assignments;
        this.sessions = sessions;
        this.enrollments = enrollments;
        this.targets = targets;
        this.attendance = attendance;
        this.scheduling = scheduling;
        this.assessments = assessments;
        this.execution = execution;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public HomeView home(Long tenantId, Long membershipId) {
        InstructorProfile instructor = instructor(tenantId, membershipId);
        Set<Long> sectionIds = sectionIds(tenantId, instructor.getId());
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(instructor.getPerson().getTenant().getTimezone())));
        List<SchedulingService.SessionView> todaySessions = sessions(tenantId, sectionIds).stream()
                .filter(session -> session.sessionDate().equals(today))
                .toList();
        List<AttendanceService.SummaryView> incomplete = attendance.list(tenantId, membershipId).stream()
                .filter(item -> !"FINALIZED".equals(item.sheetStatus()))
                .limit(8)
                .toList();
        List<AssessmentService.AssessmentView> gradingQueue = assessments.assessments(tenantId, membershipId, memberships.findByIdAndTenantId(membershipId, tenantId).orElseThrow().getUserAccount().getId())
                .stream().filter(item -> sectionIds.containsAll(item.targets().stream().map(AssessmentService.TargetView::classSectionId).toList())
                        || item.targets().stream().anyMatch(target -> sectionIds.contains(target.classSectionId())))
                .limit(8).toList();
        return new HomeView(instructor.getPerson().getFullName(), instructor.getInstructorCode(), todaySessions, incomplete, gradingQueue);
    }

    @Transactional(readOnly = true)
    public List<ClassView> classes(Long tenantId, Long membershipId) {
        InstructorProfile instructor = instructor(tenantId, membershipId);
        return assignments.findAllByTenantIdAndInstructorIdAndStatus(tenantId, instructor.getId(), ClassInstructor.Status.ACTIVE)
                .stream().map(this::classView).toList();
    }

    @Transactional(readOnly = true)
    public ClassDetail classDetail(Long tenantId, Long membershipId, Long sectionId) {
        requireSection(tenantId, membershipId, sectionId);
        ClassView meta = classes(tenantId, membershipId).stream().filter(item -> item.id().equals(sectionId)).findFirst()
                .orElseThrow(() -> notFound());
        List<EnrollmentService.View> roster = enrollments.findAllByTenantIdAndClassSectionIdOrderByStudentPersonLastNameAsc(tenantId, sectionId)
                .stream().map(InstructorPortalService::enrollmentView).toList();
        List<SchedulingService.SessionView> upcoming = sessions(tenantId, Set.of(sectionId));
        return new ClassDetail(meta, roster, upcoming);
    }

    @Transactional(readOnly = true)
    public List<SchedulingService.SessionView> timetable(Long tenantId, Long membershipId) {
        InstructorProfile instructor = instructor(tenantId, membershipId);
        return sessions(tenantId, sectionIds(tenantId, instructor.getId()));
    }

    @Transactional
    public SchedulingService.SessionView cancel(Long tenantId, Long membershipId, Long actorId, Long sessionId, String reason) {
        ClassSession session = sessions.findByIdAndTenantId(sessionId, tenantId).orElseThrow(this::notFound);
        requireSection(tenantId, membershipId, session.getClassSection().getId());
        return scheduling.cancel(tenantId, actorId, sessionId, reason);
    }

    @Transactional
    public SchedulingService.SessionView reschedule(Long tenantId, Long membershipId, Long actorId, Long sessionId,
            SchedulingService.RescheduleCommand command) {
        ClassSession session = sessions.findByIdAndTenantId(sessionId, tenantId).orElseThrow(this::notFound);
        requireSection(tenantId, membershipId, session.getClassSection().getId());
        return scheduling.reschedule(tenantId, actorId, sessionId, command);
    }

    @Transactional(readOnly = true)
    public List<AssessmentService.AssessmentView> assessments(Long tenantId, Long membershipId, Long accountId) {
        InstructorProfile instructor = instructor(tenantId, membershipId);
        Set<Long> sectionIds = sectionIds(tenantId, instructor.getId());
        return assessments.assessments(tenantId, membershipId, accountId).stream()
                .filter(item -> item.targets().stream().anyMatch(target -> sectionIds.contains(target.classSectionId())))
                .toList();
    }

    public InstructorProfile instructor(Long tenantId, Long membershipId) {
        TenantMembership membership = memberships.findByIdAndTenantId(membershipId, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBERSHIP_NOT_FOUND", "Membership was not found."));
        return instructors.findByTenantIdAndPersonId(tenantId, membership.getPerson().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "INSTRUCTOR_PROFILE_REQUIRED", "An instructor profile is required."));
    }

    public void requireSection(Long tenantId, Long membershipId, Long sectionId) {
        InstructorProfile instructor = instructor(tenantId, membershipId);
        boolean allowed = assignments.existsByClassSectionIdAndInstructorIdAndStatus(sectionId, instructor.getId(), ClassInstructor.Status.ACTIVE);
        if (!allowed) throw new ApiException(HttpStatus.FORBIDDEN, "CLASS_ACCESS_DENIED", "You are not assigned to this class.");
    }

    private Set<Long> sectionIds(Long tenantId, Long instructorId) {
        return assignments.findAllByTenantIdAndInstructorIdAndStatus(tenantId, instructorId, ClassInstructor.Status.ACTIVE)
                .stream().map(item -> item.getClassSection().getId()).collect(Collectors.toSet());
    }

    private List<SchedulingService.SessionView> sessions(Long tenantId, Set<Long> sectionIds) {
        if (sectionIds.isEmpty()) return List.of();
        Instant now = Instant.now(clock);
        return sessions.findAllByTenantIdAndClassSectionIdInOrderByPlannedStartAtAsc(tenantId, sectionIds).stream()
                .filter(session -> !session.getPlannedEndAt().isBefore(now.minusSeconds(7L * 24 * 3600)))
                .map(this::sessionView).toList();
    }

    private ClassView classView(ClassInstructor assignment) {
        ClassSection section = assignment.getClassSection();
        long students = enrollments.countActive(section.getId());
        return new ClassView(section.getId(), section.getCode(), section.getName(),
                section.getOffering().getCourse().getTitle(), section.getOffering().getBranch().getName(),
                section.getOffering().getAcademicPeriod().getId(), section.getOffering().getAcademicPeriod().getName(), section.getCapacity(), students,
                section.getDeliveryMode().name(), assignment.getRole().name());
    }

    private SchedulingService.SessionView sessionView(ClassSession s) {
        return new SchedulingService.SessionView(s.getId(), s.getClassSection().getId(), s.getClassSection().getName(),
                s.getClassSection().getOffering().getCourse().getTitle(), s.getClassSection().getOffering().getBranch().getName(),
                s.getScheduleRule() == null ? null : s.getScheduleRule().getId(), s.getSessionDate(), s.getPlannedStartAt(),
                s.getPlannedEndAt(), s.getLocation() == null ? null : s.getLocation().getName(),
                s.getInstructors().stream().map(i -> new SchedulingService.InstructorView(i.getId(), i.getPerson().getFullName())).toList(),
                s.getStatus().name(), s.isManuallyOverridden(), s.getChangeReason(),
                s.getReplacementSession() == null ? null : s.getReplacementSession().getId());
    }

    private static EnrollmentService.View enrollmentView(Enrollment e) {
        ClassSection s = e.getClassSection();
        return new EnrollmentService.View(e.getId(), e.getStudent().getId(), e.getStudent().getStudentCode(),
                e.getStudent().getPerson().getFullName(), s.getId(), s.getName(), s.getOffering().getCourse().getTitle(),
                s.getOffering().getBranch().getName(), e.getStatus().name(), e.getSource().name(), e.getStartedOn(), e.getEndedOn());
    }

    private ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "CLASS_SECTION_NOT_FOUND", "Class was not found."); }

    public record HomeView(String instructorName, String instructorCode, List<SchedulingService.SessionView> todaySessions,
            List<AttendanceService.SummaryView> incompleteAttendance, List<AssessmentService.AssessmentView> assessments) {}
    public record ClassView(Long id, String code, String name, String courseTitle, String branchName, Long academicPeriodId, String periodName,
            int capacity, long studentCount, String deliveryMode, String role) {}
    public record ClassDetail(ClassView section, List<EnrollmentService.View> students, List<SchedulingService.SessionView> sessions) {}
}
