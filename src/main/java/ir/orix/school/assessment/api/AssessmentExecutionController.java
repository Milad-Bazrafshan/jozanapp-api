package ir.orix.school.assessment.api;

import java.math.BigDecimal; import java.util.*; import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
import ir.orix.school.assessment.application.*; import ir.orix.school.shared.security.CurrentUser;

@RestController @RequestMapping("/api/v1/assessment")
public class AssessmentExecutionController {
 private final AssessmentExecutionService service; private final GradingResourceAccessGuard access; public AssessmentExecutionController(AssessmentExecutionService service,GradingResourceAccessGuard access){this.service=service;this.access=access;}
 @PostMapping("/assessments/{assessmentId}/pay") public void pay(@PathVariable Long assessmentId){service.payExamFee(CurrentUser.tenantId(),CurrentUser.membershipId(),CurrentUser.accountId(),assessmentId);}
 @PostMapping("/assessments/{assessmentId}/attempts") public AssessmentExecutionService.AttemptView start(@PathVariable Long assessmentId){return service.start(CurrentUser.tenantId(),CurrentUser.membershipId(),assessmentId);}
 @GetMapping("/attempts/{attemptId}") public AssessmentExecutionService.AttemptView attempt(@PathVariable Long attemptId){return service.get(CurrentUser.tenantId(),CurrentUser.membershipId(),attemptId);}
 @PutMapping("/attempts/{attemptId}/responses/{questionId}") public AssessmentExecutionService.AttemptView save(@PathVariable Long attemptId,@PathVariable Long questionId,@Valid @RequestBody AnswerRequest request){return service.save(CurrentUser.tenantId(),CurrentUser.membershipId(),attemptId,questionId,request.answer());}
 @PostMapping("/attempts/{attemptId}/submit") public AssessmentExecutionService.AttemptView submit(@PathVariable Long attemptId){return service.submit(CurrentUser.tenantId(),CurrentUser.membershipId(),attemptId);}
 @GetMapping("/assessments/{assessmentId}/results") @PreAuthorize("hasAuthority('GRADING_READ')") public List<AssessmentExecutionService.ResultView> results(@PathVariable Long assessmentId){return service.grading(CurrentUser.tenantId(),assessmentId);}
 @PutMapping("/responses/{responseId}/grade") @PreAuthorize("hasAuthority('GRADING_WRITE')") public AssessmentExecutionService.ResultView grade(@PathVariable Long responseId,@Valid @RequestBody GradeRequest request){access.response(CurrentUser.tenantId(),CurrentUser.membershipId(),CurrentUser.accountId(),responseId);return service.grade(CurrentUser.tenantId(),CurrentUser.accountId(),responseId,request.score(),request.feedback(),request.reason());}
 @PostMapping("/results/{resultId}/publish") @PreAuthorize("hasAuthority('RESULT_PUBLISH')") public AssessmentExecutionService.ResultView publish(@PathVariable Long resultId){access.result(CurrentUser.tenantId(),CurrentUser.membershipId(),CurrentUser.accountId(),resultId);return service.publishResult(CurrentUser.tenantId(),CurrentUser.accountId(),resultId);}
 @GetMapping("/me/results") public List<AssessmentExecutionService.ResultView> myResults(){return service.myResults(CurrentUser.tenantId(),CurrentUser.membershipId());}
 @GetMapping("/me/results/{resultId}") public AssessmentExecutionService.ResultView myResult(@PathVariable Long resultId){return service.myResult(CurrentUser.tenantId(),CurrentUser.membershipId(),resultId);}
 public record AnswerRequest(@NotNull List<@NotNull String> answer){} public record GradeRequest(@NotNull @DecimalMin("0.00") BigDecimal score,String feedback,@NotBlank @Size(max=500) String reason){}
}
