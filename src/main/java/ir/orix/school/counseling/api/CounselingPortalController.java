package ir.orix.school.counseling.api;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ir.orix.school.counseling.application.CounselingService;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/counseling/portal")
public class CounselingPortalController {
    private final CounselingService counseling;

    public CounselingPortalController(CounselingService counseling) {
        this.counseling = counseling;
    }

    @GetMapping("/daily-logs")
    public List<CounselingService.DailyLogView> dailyLogs(@RequestParam(required = false) Long studentId) {
        Long tenantId = CurrentUser.tenantId();
        Long membershipId = CurrentUser.membershipId();
        Long resolved = studentId != null ? studentId : null;
        if (resolved == null) {
            resolved = counseling.resolveStudentForPortal(tenantId, membershipId, null).getId();
        }
        return counseling.listDailyLogsForStudent(tenantId, membershipId, resolved);
    }

    @PutMapping("/daily-logs")
    public CounselingService.DailyLogView upsertDailyLog(@RequestParam(required = false) Long studentId,
            @Valid @RequestBody CounselingService.DailyLogCommand command) {
        Long tenantId = CurrentUser.tenantId();
        Long membershipId = CurrentUser.membershipId();
        Long resolved = studentId != null ? studentId : counseling.resolveStudentForPortal(tenantId, membershipId, null).getId();
        return counseling.upsertDailyLog(tenantId, membershipId, resolved, command);
    }

    @PostMapping("/daily-logs/submit")
    public CounselingService.DailyLogView submitDailyLog(@RequestParam(required = false) Long studentId,
            @RequestParam(required = false) LocalDate logDate) {
        Long tenantId = CurrentUser.tenantId();
        Long membershipId = CurrentUser.membershipId();
        Long resolved = studentId != null ? studentId : counseling.resolveStudentForPortal(tenantId, membershipId, null).getId();
        return counseling.submitDailyLog(tenantId, membershipId, resolved, logDate);
    }

    @GetMapping("/study-plans")
    public List<CounselingService.StudyPlanView> studyPlans(@RequestParam(required = false) Long studentId) {
        Long tenantId = CurrentUser.tenantId();
        Long membershipId = CurrentUser.membershipId();
        Long resolved = studentId != null ? studentId : counseling.resolveStudentForPortal(tenantId, membershipId, null).getId();
        return counseling.studyPlansForStudent(tenantId, membershipId, resolved);
    }

    @PostMapping("/study-plans/items/{itemId}/progress")
    public CounselingService.StudyPlanView recordProgress(@PathVariable Long itemId,
            @RequestParam(required = false) Long studentId, @Valid @RequestBody CounselingService.ProgressCommand command) {
        Long tenantId = CurrentUser.tenantId();
        Long membershipId = CurrentUser.membershipId();
        Long resolved = studentId != null ? studentId : counseling.resolveStudentForPortal(tenantId, membershipId, null).getId();
        return counseling.recordStudyProgress(tenantId, membershipId, resolved, itemId, command);
    }
}
