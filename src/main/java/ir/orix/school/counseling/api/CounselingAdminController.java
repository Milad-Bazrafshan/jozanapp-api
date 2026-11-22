package ir.orix.school.counseling.api;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ir.orix.school.counseling.application.CounselingService;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/counseling")
public class CounselingAdminController {
    private final CounselingService counseling;

    public CounselingAdminController(CounselingService counseling) {
        this.counseling = counseling;
    }

    @GetMapping("/supervisions")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('COUNSELING_ASSIGN')")
    public List<CounselingService.SupervisionView> supervisions() {
        return counseling.listSupervisions(CurrentUser.tenantId());
    }

    @GetMapping("/assignments")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('COUNSELING_ASSIGN')")
    public List<CounselingService.AssignmentView> assignments() {
        return counseling.listAssignments(CurrentUser.tenantId());
    }

    @PostMapping("/assignments")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('COUNSELING_ASSIGN')")
    public CounselingService.AssignmentView createAssignment(@Valid @RequestBody CounselingService.AssignmentCommand command) {
        return counseling.createAssignment(CurrentUser.tenantId(), CurrentUser.accountId(), command);
    }

    @PostMapping("/assignments/{id}/end")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('COUNSELING_ASSIGN')")
    public CounselingService.AssignmentView endAssignment(@PathVariable Long id) {
        return counseling.endAssignment(CurrentUser.tenantId(), CurrentUser.accountId(), id);
    }

    @PostMapping("/supervisions")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('COUNSELING_ASSIGN')")
    public CounselingService.SupervisionView createSupervision(@Valid @RequestBody CounselingService.SupervisionCommand command) {
        return counseling.createSupervision(CurrentUser.tenantId(), CurrentUser.accountId(), command);
    }

    @GetMapping("/admin-reports")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('COUNSELING_ADMIN_REPORT_READ')")
    public List<CounselingService.AdminReportView> adminReports() {
        return counseling.listAdminReports(CurrentUser.tenantId());
    }

    public record DraftSeniorReportRequest(@NotNull Long counselorMembershipId, int year, int month) {}

    @PostMapping("/senior-reports/draft")
    @PreAuthorize("hasAuthority('COUNSELING_SENIOR_REPORT_READ')")
    public CounselingService.SeniorReportView draftSeniorReport(@Valid @RequestBody DraftSeniorReportRequest request) {
        return counseling.draftMonthlySeniorReportForCounselor(CurrentUser.tenantId(), CurrentUser.membershipId(),
                request.counselorMembershipId(), request.year(), request.month());
    }

    @GetMapping("/senior-reports")
    @PreAuthorize("hasAuthority('COUNSELING_SENIOR_REPORT_READ')")
    public List<CounselingService.SeniorReportView> seniorReports() {
        return counseling.listSeniorReports(CurrentUser.tenantId(), CurrentUser.membershipId());
    }

    @PostMapping("/senior-reports/{id}/ai-analysis")
    @PreAuthorize("hasAuthority('COUNSELING_SENIOR_REPORT_READ')")
    public CounselingService.SeniorReportView aiAnalysis(@PathVariable Long id, @RequestBody(required = false) AiAnalysisRequest request) {
        if (request != null && request.analysisText() != null && !request.analysisText().isBlank()) {
            return counseling.applyAiAnalysis(CurrentUser.tenantId(), CurrentUser.membershipId(), id, request.analysisText());
        }
        return counseling.regenerateAiAnalysis(CurrentUser.tenantId(), CurrentUser.membershipId(), id);
    }

    @PostMapping("/senior-reports/{id}/finalize")
    @PreAuthorize("hasAuthority('COUNSELING_SENIOR_REPORT_READ')")
    public CounselingService.SeniorReportView finalizeSenior(@PathVariable Long id) {
        return counseling.finalizeSeniorReport(CurrentUser.tenantId(), CurrentUser.membershipId(), id);
    }

    public record AiAnalysisRequest(String analysisText) {}
}
