package ir.orix.school.survey.api;

import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ir.orix.school.people.application.PeopleService;
import ir.orix.school.shared.security.CurrentUser;
import ir.orix.school.survey.application.SurveyService;
import ir.orix.school.survey.domain.Survey;

@RestController
@RequestMapping("/api/v1")
public class SurveyController {
    private final SurveyService surveys;
    private final PeopleService people;
    public SurveyController(SurveyService surveys, PeopleService people) { this.surveys = surveys; this.people = people; }

    @GetMapping("/surveys")
    @PreAuthorize("hasAuthority('SURVEY_READ')")
    public List<SurveyService.SurveyView> list() { return surveys.list(CurrentUser.tenantId()); }

    @PostMapping("/surveys")
    @PreAuthorize("hasAuthority('SURVEY_WRITE')")
    public SurveyService.SurveyView create(@Valid @RequestBody CreateRequest request) {
        return surveys.create(CurrentUser.tenantId(), CurrentUser.accountId(), request.toCommand());
    }

    @GetMapping("/surveys/{id}/report")
    @PreAuthorize("hasAuthority('SURVEY_READ')")
    public SurveyService.Report report(@PathVariable Long id) { return surveys.report(CurrentUser.tenantId(), id); }

    @GetMapping("/surveys/available")
    public List<SurveyService.SurveyView> available(@RequestParam(required = false) Long studentId) {
        Long target = resolveStudent(studentId);
        return surveys.available(CurrentUser.tenantId(), target);
    }

    @PostMapping("/surveys/{id}/responses")
    public void submit(@PathVariable Long id, @Valid @RequestBody SubmitRequest request) {
        Long target = resolveStudent(request.studentId());
        surveys.submit(CurrentUser.tenantId(), CurrentUser.membershipId(), id, target, request.answers());
    }

    private Long resolveStudent(Long studentId) {
        PeopleService.MyProfiles profiles = people.myProfiles(CurrentUser.tenantId(), CurrentUser.membershipId());
        if (studentId != null) {
            people.requireChildAcademicAccess(CurrentUser.tenantId(), CurrentUser.membershipId(), studentId);
            return studentId;
        }
        if (profiles.student() == null) throw new ir.orix.school.shared.web.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "STUDENT_REQUIRED", "Student is required.");
        return profiles.student().id();
    }

    public record CreateRequest(@NotNull Long instructorId, @NotNull Long classSectionId, @NotBlank String title,
            @NotNull Survey.Audience audience, @NotNull Instant opensAt, @NotNull Instant closesAt, @NotNull List<@NotBlank String> questions) {
        SurveyService.Command toCommand() { return new SurveyService.Command(instructorId, classSectionId, title, audience, opensAt, closesAt, questions); }
    }
    public record SubmitRequest(Long studentId, @NotNull List<SurveyService.AnswerCommand> answers) {}
}
