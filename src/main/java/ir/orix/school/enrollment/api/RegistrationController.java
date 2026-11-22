package ir.orix.school.enrollment.api;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import ir.orix.school.enrollment.application.EnrollmentService;
import ir.orix.school.enrollment.application.RegistrationService;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/registration")
public class RegistrationController {
    private final RegistrationService registration;
    public RegistrationController(RegistrationService registration) { this.registration = registration; }

    @GetMapping("/offerings")
    public List<RegistrationService.OfferingView> offerings() {
        return registration.catalog(CurrentUser.tenantId());
    }

    @PostMapping("/enroll")
    public EnrollmentService.View enroll(@Valid @RequestBody EnrollRequest request) {
        return registration.register(CurrentUser.tenantId(), CurrentUser.membershipId(), CurrentUser.accountId(),
                request.studentId(), request.classSectionId(), Boolean.TRUE.equals(request.allowWaitlist()));
    }

    public record EnrollRequest(@NotNull Long studentId, @NotNull Long classSectionId, Boolean allowWaitlist) {}
}
