package ir.orix.school.portal.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ir.orix.school.identity.application.TwoFactorSmsService.SendCodeResult;
import ir.orix.school.portal.application.AccountPortalService;
import ir.orix.school.portal.application.AccountPortalService.AccountSummary;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/portal/account")
public class AccountPortalController {
    private final AccountPortalService accountPortalService;

    public AccountPortalController(AccountPortalService accountPortalService) {
        this.accountPortalService = accountPortalService;
    }

    @GetMapping
    public AccountSummary get() {
        return accountPortalService.accountSummary(CurrentUser.membershipId(), CurrentUser.tenantId());
    }

    @PostMapping("/mobile")
    public void updateMobile(@Valid @RequestBody MobileRequest request) {
        accountPortalService.updateMobile(CurrentUser.membershipId(), CurrentUser.tenantId(), request.mobile());
    }

    @PostMapping("/password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        accountPortalService.changePassword(
                CurrentUser.membershipId(), CurrentUser.tenantId(), request.currentPassword(), request.newPassword());
    }

    @PostMapping("/two-factor/send-enable")
    public SendCodeResult sendEnable() {
        return accountPortalService.requestTwoFactorEnable(CurrentUser.membershipId(), CurrentUser.tenantId());
    }

    @PostMapping("/two-factor/confirm-enable")
    public void confirmEnable(@Valid @RequestBody TwoFactorConfirmRequest request) {
        accountPortalService.confirmTwoFactorEnable(
                CurrentUser.membershipId(), CurrentUser.tenantId(), request.challengeId(), request.code());
    }

    @PostMapping("/two-factor/send-disable")
    public SendCodeResult sendDisable() {
        return accountPortalService.requestTwoFactorDisable(CurrentUser.membershipId(), CurrentUser.tenantId());
    }

    @PostMapping("/two-factor/confirm-disable")
    public void confirmDisable(@Valid @RequestBody TwoFactorConfirmRequest request) {
        accountPortalService.confirmTwoFactorDisable(
                CurrentUser.membershipId(), CurrentUser.tenantId(), request.challengeId(), request.code());
    }

    public record MobileRequest(@NotBlank @Size(max = 30) String mobile) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 10, max = 128) String newPassword) {
    }

    public record TwoFactorConfirmRequest(@NotNull Long challengeId, @NotBlank @Size(min = 4, max = 10) String code) {
    }
}
