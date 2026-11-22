package ir.orix.school.reportcard.api;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ir.orix.school.people.application.PeopleService;
import ir.orix.school.reportcard.application.ReportCardService;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1")
public class ReportCardController {
    private final ReportCardService service;
    private final PeopleService people;
    public ReportCardController(ReportCardService service, PeopleService people) {
        this.service = service;
        this.people = people;
    }

    @GetMapping("/report-cards/runs")
    @PreAuthorize("hasAuthority('REPORT_CARD_READ')")
    public List<ReportCardService.RunView> runs() { return service.list(CurrentUser.tenantId()); }

    @PostMapping("/report-cards/runs")
    @PreAuthorize("hasAuthority('REPORT_CARD_WRITE')")
    public ReportCardService.RunView generate(@Valid @RequestBody GenerateRequest request) {
        return service.generate(CurrentUser.tenantId(), CurrentUser.accountId(), CurrentUser.membershipId(), request.toCommand(), false);
    }

    @PostMapping("/report-cards/runs/{id}/finalize")
    @PreAuthorize("hasAuthority('REPORT_CARD_WRITE')")
    public ReportCardService.RunView finalize(@PathVariable Long id) {
        return service.finalize(CurrentUser.tenantId(), CurrentUser.accountId(), id);
    }

    @PostMapping("/report-cards/runs/{id}/publish")
    @PreAuthorize("hasAuthority('REPORT_CARD_PUBLISH')")
    public ReportCardService.RunView publish(@PathVariable Long id) {
        return service.publish(CurrentUser.tenantId(), CurrentUser.accountId(), id);
    }

    @GetMapping("/report-cards/runs/{id}/cards")
    @PreAuthorize("hasAuthority('REPORT_CARD_READ')")
    public List<ReportCardService.CardView> cards(@PathVariable Long id) {
        return service.cards(CurrentUser.tenantId(), id);
    }

    @GetMapping(value = "/report-cards/{id}/print", produces = MediaType.TEXT_HTML_VALUE)
    public String print(@PathVariable Long id) {
        return service.printHtml(CurrentUser.tenantId(), id);
    }

    @GetMapping("/report-cards/me")
    public List<ReportCardService.CardView> mine() {
        PeopleService.MyProfiles profiles = people.myProfiles(CurrentUser.tenantId(), CurrentUser.membershipId());
        if (profiles.student() == null) throw new ir.orix.school.shared.web.ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "STUDENT_PROFILE_REQUIRED", "Student profile is required.");
        return service.mine(CurrentUser.tenantId(), profiles.student().id());
    }

    @GetMapping("/guardian/children/{studentId}/report-cards")
    public List<ReportCardService.CardView> child(@PathVariable Long studentId) {
        people.requireChildAcademicAccess(CurrentUser.tenantId(), CurrentUser.membershipId(), studentId);
        return service.mine(CurrentUser.tenantId(), studentId);
    }

    @PostMapping("/instructor/report-cards/runs")
    public ReportCardService.RunView instructorGenerate(@Valid @RequestBody GenerateRequest request) {
        return service.generate(CurrentUser.tenantId(), CurrentUser.accountId(), CurrentUser.membershipId(), request.toCommand(), true);
    }

    @GetMapping("/public/report-cards/{token}")
    public ReportCardService.CardView verify(@PathVariable String token) { return service.verify(token); }

    @GetMapping(value = "/public/report-cards/{token}/print", produces = MediaType.TEXT_HTML_VALUE)
    public String printPublic(@PathVariable String token) { return service.printPublic(token); }

    public record GenerateRequest(@NotNull Long academicPeriodId, @NotNull Long classSectionId, @NotBlank String title, boolean includeRank) {
        ReportCardService.Command toCommand() { return new ReportCardService.Command(academicPeriodId, classSectionId, title, includeRank); }
    }
}
