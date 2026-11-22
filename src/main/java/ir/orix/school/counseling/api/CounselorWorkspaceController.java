package ir.orix.school.counseling.api;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ir.orix.school.counseling.application.CounselingService;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/counselor/counseling")
public class CounselorWorkspaceController {
    private final CounselingService counseling;

    public CounselorWorkspaceController(CounselingService counseling) {
        this.counseling = counseling;
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasAuthority('COUNSELING_READ')")
    public List<CounselingService.AssignmentView> assignments() {
        return counseling.listAssignmentsForCounselor(CurrentUser.tenantId(), CurrentUser.membershipId());
    }

    @GetMapping("/daily-logs/inbox")
    @PreAuthorize("hasAuthority('COUNSELING_READ')")
    public List<CounselingService.DailyLogView> inbox() {
        return counseling.counselorDailyInbox(CurrentUser.tenantId(), CurrentUser.membershipId());
    }

    @PostMapping("/daily-logs/{id}/review")
    @PreAuthorize("hasAuthority('COUNSELING_WRITE')")
    public CounselingService.DailyLogView review(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return counseling.reviewDailyLog(CurrentUser.tenantId(), CurrentUser.membershipId(), id, request.feedback());
    }

    @GetMapping("/study-plans")
    @PreAuthorize("hasAuthority('COUNSELING_READ')")
    public List<CounselingService.StudyPlanView> studyPlans() {
        return counseling.studyPlansForCounselor(CurrentUser.tenantId(), CurrentUser.membershipId());
    }

    @PostMapping("/study-plans")
    @PreAuthorize("hasAuthority('COUNSELING_WRITE')")
    public CounselingService.StudyPlanView createPlan(@Valid @RequestBody CounselingService.StudyPlanCommand command) {
        return counseling.createStudyPlan(CurrentUser.tenantId(), CurrentUser.membershipId(), CurrentUser.accountId(), command);
    }

    @DeleteMapping("/study-plans/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('COUNSELING_WRITE')")
    public void deletePlanItem(@PathVariable Long itemId) {
        counseling.deleteStudyPlanItem(CurrentUser.tenantId(), CurrentUser.membershipId(), CurrentUser.accountId(), itemId);
    }

    @GetMapping("/admin-reports/mine")
    @PreAuthorize("hasAuthority('COUNSELING_READ')")
    public List<CounselingService.AdminReportView> myAdminReports() {
        return counseling.listCounselorAdminReports(CurrentUser.tenantId(), CurrentUser.membershipId());
    }

    @PostMapping("/admin-reports")
    @PreAuthorize("hasAuthority('COUNSELING_WRITE')")
    public CounselingService.AdminReportView createAdminReport(@Valid @RequestBody CounselingService.AdminReportCommand command) {
        return counseling.createAdminReport(CurrentUser.tenantId(), CurrentUser.membershipId(), command);
    }

    @PostMapping("/admin-reports/{id}/send")
    @PreAuthorize("hasAuthority('COUNSELING_WRITE')")
    public CounselingService.AdminReportView sendAdminReport(@PathVariable Long id) {
        return counseling.sendAdminReport(CurrentUser.tenantId(), CurrentUser.membershipId(), id);
    }

    public record ReviewRequest(@NotBlank String feedback) {}
}
