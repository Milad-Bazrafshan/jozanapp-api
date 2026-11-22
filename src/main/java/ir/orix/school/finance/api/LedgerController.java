package ir.orix.school.finance.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ir.orix.school.finance.ledger.application.LedgerService;
import ir.orix.school.finance.ledger.domain.LedgerAccount;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/finance")
public class LedgerController {
    private final LedgerService ledger;
    public LedgerController(LedgerService ledger) { this.ledger = ledger; }

    @GetMapping("/accounts")
    @PreAuthorize("hasAuthority('LEDGER_READ')")
    public List<LedgerService.AccountView> accounts() { return ledger.accounts(CurrentUser.tenantId()); }

    @PostMapping("/accounts")
    @PreAuthorize("hasAuthority('LEDGER_WRITE')")
    public LedgerService.AccountView account(@Valid @RequestBody AccountRequest request) {
        return ledger.createAccount(CurrentUser.tenantId(), CurrentUser.accountId(), request.code(), request.name(), request.type(), request.parentId());
    }

    @GetMapping("/voucher-types")
    @PreAuthorize("hasAuthority('LEDGER_READ')")
    public List<LedgerService.TypeView> types() { return ledger.types(CurrentUser.tenantId()); }

    @GetMapping("/vouchers")
    @PreAuthorize("hasAuthority('LEDGER_READ')")
    public List<LedgerService.VoucherView> vouchers() { return ledger.vouchers(CurrentUser.tenantId()); }

    @PostMapping("/vouchers")
    @PreAuthorize("hasAuthority('LEDGER_WRITE')")
    public LedgerService.VoucherView create(@Valid @RequestBody VoucherRequest request) {
        return ledger.create(CurrentUser.tenantId(), CurrentUser.accountId(), request.toCommand());
    }

    @PostMapping("/vouchers/{id}/post")
    @PreAuthorize("hasAuthority('LEDGER_WRITE')")
    public LedgerService.VoucherView post(@PathVariable Long id) {
        return ledger.post(CurrentUser.tenantId(), CurrentUser.accountId(), id);
    }

    @PostMapping("/vouchers/{id}/void")
    @PreAuthorize("hasAuthority('LEDGER_WRITE')")
    public LedgerService.VoucherView voidVoucher(@PathVariable Long id) {
        return ledger.voidVoucher(CurrentUser.tenantId(), CurrentUser.accountId(), id);
    }

    @GetMapping(value = "/vouchers/{id}/print", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasAuthority('LEDGER_READ')")
    public String print(@PathVariable Long id) { return ledger.print(CurrentUser.tenantId(), id); }

    @GetMapping("/trial-balance")
    @PreAuthorize("hasAuthority('LEDGER_READ')")
    public List<LedgerService.TrialRow> trial() { return ledger.trialBalance(CurrentUser.tenantId()); }

    public record AccountRequest(@NotBlank String code, @NotBlank String name, @NotNull LedgerAccount.Type type, Long parentId) {}
    public record LineRequest(@NotNull Long accountId, String description, BigDecimal debit, BigDecimal credit) {}
    public record VoucherRequest(@NotNull Long voucherTypeId, @NotNull LocalDate voucherDate, String description, boolean post,
            @NotNull List<@Valid LineRequest> lines) {
        LedgerService.VoucherCommand toCommand() {
            return new LedgerService.VoucherCommand(voucherTypeId, voucherDate, description, post,
                    lines.stream().map(line -> new LedgerService.LineCommand(line.accountId(), line.description(), line.debit(), line.credit())).toList());
        }
    }
}
