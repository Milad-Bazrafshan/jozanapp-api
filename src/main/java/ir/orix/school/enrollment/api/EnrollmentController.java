package ir.orix.school.enrollment.api;
import java.time.LocalDate; import java.util.*; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import org.springframework.http.ResponseEntity; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import ir.orix.school.enrollment.application.EnrollmentService; import ir.orix.school.shared.security.CurrentUser;
@RestController @RequestMapping("/api/v1/enrollments")
public class EnrollmentController {
 private final EnrollmentService service;public EnrollmentController(EnrollmentService service){this.service=service;}
 @GetMapping @PreAuthorize("@scopeAuthorizer.hasTenantPermission('ENROLLMENT_READ')") public List<EnrollmentService.View> list(){return service.list(CurrentUser.tenantId());}
 @GetMapping("/me") public List<EnrollmentService.View> mine(){return service.mine(CurrentUser.tenantId(),CurrentUser.membershipId());}
 @PostMapping @PreAuthorize("@scopeAuthorizer.hasTenantPermission('ENROLLMENT_WRITE')") public EnrollmentService.View enroll(@Valid @RequestBody EnrollRequest r){return service.enroll(CurrentUser.tenantId(),CurrentUser.accountId(),new EnrollmentService.Command(r.studentId(),r.classSectionId(),r.startedOn(),r.allowWaitlist(),r.reason()));}
 @PostMapping("/{id}/cancel") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('ENROLLMENT_WRITE')") public EnrollmentService.View cancel(@PathVariable Long id,@Valid @RequestBody CancelRequest r){return service.cancel(CurrentUser.tenantId(),CurrentUser.accountId(),id,r.reason());}
 @PostMapping("/{id}/transfer") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('ENROLLMENT_WRITE')") public EnrollmentService.View transfer(@PathVariable Long id,@Valid @RequestBody TransferRequest r){return service.transfer(CurrentUser.tenantId(),CurrentUser.accountId(),id,new EnrollmentService.TransferCommand(r.targetClassSectionId(),r.transferOn(),r.reason()));}
 public record EnrollRequest(@NotNull Long studentId,@NotNull Long classSectionId,LocalDate startedOn,boolean allowWaitlist,@Size(max=500) String reason){} public record CancelRequest(@NotBlank @Size(max=500) String reason){} public record TransferRequest(@NotNull Long targetClassSectionId,LocalDate transferOn,@NotBlank @Size(max=500) String reason){}
}
