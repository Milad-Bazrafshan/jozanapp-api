package ir.orix.school.portal.api;

import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ir.orix.school.finance.application.WalletService;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/guardian/wallet")
public class GuardianWalletController {
    private final WalletService wallets;

    public GuardianWalletController(WalletService wallets) {
        this.wallets = wallets;
    }

    @GetMapping
    public WalletService.WalletView wallet() {
        return wallets.mine(CurrentUser.tenantId(), CurrentUser.membershipId());
    }

    @PostMapping("/top-ups")
    public WalletService.WalletView topUp(@Valid @RequestBody TopUpRequest request) {
        return wallets.topUp(CurrentUser.tenantId(), CurrentUser.membershipId(), CurrentUser.accountId(),
                request.amount(), request.notes());
    }

    public record TopUpRequest(@NotNull @DecimalMin("1000.00") BigDecimal amount, @Size(max = 500) String notes) {}
}
