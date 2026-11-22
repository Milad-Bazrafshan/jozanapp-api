package ir.orix.school.instructor.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import ir.orix.school.assessment.application.AssessmentExecutionService;
import ir.orix.school.assessment.application.AssessmentService;
import ir.orix.school.attendance.application.AttendanceService;
import ir.orix.school.attendance.domain.AttendanceRecord;
import ir.orix.school.classroom.domain.ClassSection;
import ir.orix.school.instructor.application.InstructorPortalService;
import ir.orix.school.scheduling.application.SchedulingService;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/instructor")
public class InstructorPortalController {
    private final InstructorPortalService portal;
    private final AttendanceService attendance;
    private final AssessmentExecutionService execution;

    public InstructorPortalController(InstructorPortalService portal, AttendanceService attendance,
            AssessmentExecutionService execution) {
        this.portal = portal;
        this.attendance = attendance;
        this.execution = execution;
    }

    @GetMapping("/home")
    public InstructorPortalService.HomeView home() {
        return portal.home(CurrentUser.tenantId(), CurrentUser.membershipId());
    }

    @GetMapping("/classes")
    public List<InstructorPortalService.ClassView> classes() {
        return portal.classes(CurrentUser.tenantId(), CurrentUser.membershipId());
    }

    @GetMapping("/classes/{id}")
    public InstructorPortalService.ClassDetail classDetail(@PathVariable Long id) {
        return portal.classDetail(CurrentUser.tenantId(), CurrentUser.membershipId(), id);
    }

    @GetMapping("/sessions")
    public List<SchedulingService.SessionView> sessions() {
        return portal.timetable(CurrentUser.tenantId(), CurrentUser.membershipId());
    }

    @PostMapping("/sessions/{id}/cancel")
    public SchedulingService.SessionView cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return portal.cancel(CurrentUser.tenantId(), CurrentUser.membershipId(), CurrentUser.accountId(), id, request.reason());
    }

    @PostMapping("/sessions/{id}/reschedule")
    public SchedulingService.SessionView reschedule(@PathVariable Long id, @Valid @RequestBody RescheduleRequest request) {
        return portal.reschedule(CurrentUser.tenantId(), CurrentUser.membershipId(), CurrentUser.accountId(), id,
                new SchedulingService.RescheduleCommand(request.sessionDate(), request.startsAt(), request.endsAt(), request.timeZone(), request.reason()));
    }

    @GetMapping("/attendance")
    public List<AttendanceService.SummaryView> attendance() {
        portal.instructor(CurrentUser.tenantId(), CurrentUser.membershipId());
        return attendance.list(CurrentUser.tenantId(), CurrentUser.membershipId());
    }

    @GetMapping("/attendance/{sessionId}")
    public AttendanceService.SheetView sheet(@PathVariable Long sessionId) {
        return attendance.sheet(CurrentUser.tenantId(), CurrentUser.membershipId(), sessionId);
    }

    @PutMapping("/attendance/{sessionId}")
    public AttendanceService.SheetView save(@PathVariable Long sessionId, @Valid @RequestBody SaveAttendanceRequest request) {
        return attendance.save(CurrentUser.tenantId(), CurrentUser.membershipId(), CurrentUser.accountId(), sessionId,
                new AttendanceService.SaveCommand(request.finalizeSheet(), request.correctionReason(),
                        request.records().stream().map(r -> new AttendanceService.RecordCommand(r.studentId(), r.status(), r.deliveryMode(), r.lateMinutes(), r.note())).toList()));
    }

    @GetMapping("/assessments")
    public List<AssessmentService.AssessmentView> assessments() {
        return portal.assessments(CurrentUser.tenantId(), CurrentUser.membershipId(), CurrentUser.accountId());
    }

    @GetMapping("/assessments/{id}/results")
    public List<AssessmentExecutionService.ResultView> results(@PathVariable Long id) {
        return execution.grading(CurrentUser.tenantId(), id);
    }

    public record ReasonRequest(@NotBlank @Size(max = 500) String reason) {}
    public record RescheduleRequest(@NotNull LocalDate sessionDate, @NotNull LocalTime startsAt, @NotNull LocalTime endsAt,
            @NotBlank String timeZone, @NotBlank @Size(max = 500) String reason) {}
    public record RecordRequest(@NotNull Long studentId, @NotNull AttendanceRecord.PresenceStatus status,
            @NotNull ClassSection.DeliveryMode deliveryMode, Integer lateMinutes, String note) {}
    public record SaveAttendanceRequest(boolean finalizeSheet, String correctionReason, @NotNull List<@Valid RecordRequest> records) {}
}
