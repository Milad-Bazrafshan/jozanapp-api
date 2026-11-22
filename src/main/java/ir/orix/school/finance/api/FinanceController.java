package ir.orix.school.finance.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ir.orix.school.finance.application.FinanceService;
import ir.orix.school.finance.domain.Charge;
import ir.orix.school.finance.domain.Payment;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController {
    private final FinanceService service;
    public FinanceController(FinanceService service) { this.service = service; }

    @GetMapping("/overview")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_READ')")
    public FinanceService.Overview overview() { return service.overview(CurrentUser.tenantId()); }

    @GetMapping("/charges")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_READ')")
    public List<FinanceService.ChargeView> charges() { return service.charges(CurrentUser.tenantId()); }

    @PostMapping("/charges")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_WRITE')")
    public FinanceService.ChargeView charge(@Valid @RequestBody ChargeRequest request) {
        return service.createCharge(CurrentUser.tenantId(), CurrentUser.accountId(), request.toCommand());
    }

    @PostMapping("/charges/plans")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_WRITE')")
    public List<FinanceService.ChargeView> plan(@Valid @RequestBody PlanRequest request) {
        return service.createPlan(CurrentUser.tenantId(), CurrentUser.accountId(), request.toCommand());
    }

    @PostMapping("/charges/{id}/void")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_WRITE')")
    public FinanceService.ChargeView voidCharge(@PathVariable Long id) {
        return service.voidCharge(CurrentUser.tenantId(), CurrentUser.accountId(), id);
    }

    @GetMapping("/invoices")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_READ')")
    public List<FinanceService.InvoiceView> invoices() { return service.invoices(CurrentUser.tenantId()); }

    @PostMapping("/invoices")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_WRITE')")
    public FinanceService.InvoiceView invoice(@Valid @RequestBody InvoiceRequest request) {
        return service.createInvoice(CurrentUser.tenantId(), CurrentUser.accountId(), request.toCommand());
    }

    @PostMapping("/invoices/{id}/issue")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_WRITE')")
    public FinanceService.InvoiceView issue(@PathVariable Long id) {
        return service.issueInvoice(CurrentUser.tenantId(), CurrentUser.accountId(), id);
    }

    @PostMapping("/invoices/{id}/void")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_WRITE')")
    public FinanceService.InvoiceView voidInvoice(@PathVariable Long id) {
        return service.voidInvoice(CurrentUser.tenantId(), CurrentUser.accountId(), id);
    }

    @GetMapping("/payments")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_READ')")
    public List<FinanceService.PaymentView> payments() { return service.payments(CurrentUser.tenantId()); }

    @PostMapping("/payments")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('PAYMENT_WRITE')")
    public FinanceService.PaymentView payment(@Valid @RequestBody PaymentRequest request) {
        return service.recordPayment(CurrentUser.tenantId(), CurrentUser.accountId(), request.toCommand());
    }

    @PostMapping("/payments/{id}/allocations")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('PAYMENT_WRITE')")
    public FinanceService.PaymentView allocate(@PathVariable Long id, @Valid @RequestBody AllocateRequest request) {
        return service.allocate(CurrentUser.tenantId(), CurrentUser.accountId(), id, request.toCommands());
    }

    @GetMapping("/balances")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_READ')")
    public List<FinanceService.BalanceView> balances() { return service.balances(CurrentUser.tenantId()); }

    @GetMapping("/students/{studentId}/statement")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_READ')")
    public FinanceService.Statement statement(@PathVariable Long studentId) {
        return service.statement(CurrentUser.tenantId(), studentId);
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_READ')")
    public FinanceService.InvoiceView invoice(@PathVariable Long id) { return service.invoice(CurrentUser.tenantId(), id); }

    @GetMapping(value = "/invoices/{id}/print", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_READ')")
    public String printInvoice(@PathVariable Long id) { return service.printInvoice(CurrentUser.tenantId(), id); }

    @GetMapping("/payments/{id}")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_READ')")
    public FinanceService.PaymentView payment(@PathVariable Long id) { return service.payment(CurrentUser.tenantId(), id); }

    @GetMapping(value = "/payments/{id}/receipt", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_READ')")
    public String receipt(@PathVariable Long id) { return service.printReceipt(CurrentUser.tenantId(), id); }

    @PostMapping("/payments/{id}/refund")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('PAYMENT_WRITE')")
    public FinanceService.PaymentView refund(@PathVariable Long id, @Valid @RequestBody ReasonBody body) {
        return service.refund(CurrentUser.tenantId(), CurrentUser.accountId(), id, body.reason());
    }

    @GetMapping("/reports")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('FINANCE_READ')")
    public FinanceService.OpsReport reports() { return service.opsReport(CurrentUser.tenantId()); }

    public record ReasonBody(@Size(max = 500) String reason) {}

    public record ChargeRequest(@NotNull Long studentId, Long classSectionId, Long enrollmentId, @NotNull Charge.Type type,
            @NotBlank @Size(max = 200) String title, @NotNull @DecimalMin("0.01") BigDecimal amount,
            @DecimalMin("0") BigDecimal discount, @NotNull LocalDate dueOn, @Size(max = 500) String notes,
            @Size(max = 60) String code) {
        FinanceService.ChargeCommand toCommand() {
            return new FinanceService.ChargeCommand(studentId, classSectionId, enrollmentId, type, title, amount,
                    discount == null ? BigDecimal.ZERO : discount, dueOn, notes, code);
        }
    }

    public record PlanRequest(@NotNull Long studentId, Long classSectionId, Long enrollmentId, @NotNull Charge.Type type,
            @NotBlank @Size(max = 200) String title, @NotNull @DecimalMin("0.01") BigDecimal amount,
            @DecimalMin("0") BigDecimal discount, @NotNull LocalDate firstDueOn, @Min(2) @Max(24) int installmentCount,
            @Min(1) int intervalDays, @Size(max = 500) String notes) {
        FinanceService.PlanCommand toCommand() {
            return new FinanceService.PlanCommand(studentId, classSectionId, enrollmentId, type, title, amount,
                    discount == null ? BigDecimal.ZERO : discount, firstDueOn, installmentCount, intervalDays, notes);
        }
    }

    public record InvoiceRequest(@NotNull Long studentId, @NotNull LocalDate dueOn, @Size(max = 500) String notes,
            @NotNull List<Long> chargeIds, @Size(max = 60) String code) {
        FinanceService.InvoiceCommand toCommand() {
            return new FinanceService.InvoiceCommand(studentId, dueOn, notes, chargeIds, code);
        }
    }

    public record AllocationItem(@NotNull Long chargeId, @NotNull @DecimalMin("0.01") BigDecimal amount) {}

    public record PaymentRequest(@NotNull Long studentId, @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull Payment.Method method, Payment.Provider provider, @Size(max = 120) String externalReference,
            Instant paidAt, @Size(max = 500) String notes, @Size(max = 60) String receiptNumber,
            List<@Valid AllocationItem> allocations) {
        FinanceService.PaymentCommand toCommand() {
            List<FinanceService.AllocationCommand> items = allocations == null ? List.of()
                    : allocations.stream().map(item -> new FinanceService.AllocationCommand(item.chargeId(), item.amount())).toList();
            return new FinanceService.PaymentCommand(studentId, amount, method, provider, externalReference, paidAt, notes, receiptNumber, items);
        }
    }

    public record AllocateRequest(@NotNull List<@Valid AllocationItem> allocations) {
        List<FinanceService.AllocationCommand> toCommands() {
            return allocations.stream().map(item -> new FinanceService.AllocationCommand(item.chargeId(), item.amount())).toList();
        }
    }
}
