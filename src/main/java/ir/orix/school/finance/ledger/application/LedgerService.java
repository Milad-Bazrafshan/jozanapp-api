package ir.orix.school.finance.ledger.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.finance.domain.Invoice;
import ir.orix.school.finance.domain.Payment;
import ir.orix.school.finance.ledger.domain.JournalVoucher;
import ir.orix.school.finance.ledger.domain.JournalVoucherLine;
import ir.orix.school.finance.ledger.domain.LedgerAccount;
import ir.orix.school.finance.ledger.domain.VoucherType;
import ir.orix.school.finance.ledger.persistence.JournalVoucherRepository;
import ir.orix.school.finance.ledger.persistence.LedgerAccountRepository;
import ir.orix.school.finance.ledger.persistence.VoucherTypeRepository;
import ir.orix.school.identity.persistence.UserAccountRepository;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.shared.web.ApiException;
import ir.orix.school.shared.web.PrintDocuments;

@Service
public class LedgerService {
    private final TenantRepository tenants;
    private final LedgerAccountRepository accounts;
    private final VoucherTypeRepository types;
    private final JournalVoucherRepository vouchers;
    private final UserAccountRepository users;
    private final AuditService audit;
    private final Clock clock;

    public LedgerService(TenantRepository tenants, LedgerAccountRepository accounts, VoucherTypeRepository types,
            JournalVoucherRepository vouchers, UserAccountRepository users, AuditService audit, Clock clock) {
        this.tenants = tenants; this.accounts = accounts; this.types = types; this.vouchers = vouchers; this.users = users;
        this.audit = audit; this.clock = clock;
    }

    @Transactional
    public void ensureDefaults(Long tenantId) {
        Tenant tenant = tenants.findById(tenantId).orElseThrow();
        seedAccount(tenant, "1100", "صندوق و بانک", LedgerAccount.Type.ASSET);
        seedAccount(tenant, "1200", "حساب‌های دریافتنی شهریه", LedgerAccount.Type.ASSET);
        seedAccount(tenant, "2100", "بدهی‌ها", LedgerAccount.Type.LIABILITY);
        seedAccount(tenant, "4100", "درآمد شهریه", LedgerAccount.Type.REVENUE);
        seedAccount(tenant, "5100", "هزینه استرداد", LedgerAccount.Type.EXPENSE);
        seedType(tenant, "INV", "صدور فاکتور", true);
        seedType(tenant, "PAY", "دریافت وجه", true);
        seedType(tenant, "REF", "استرداد", true);
        seedType(tenant, "ADJ", "سند تعدیل", false);
        seedType(tenant, "OPN", "افتتاحیه", false);
        seedType(tenant, "CLS", "اختتامیه", false);
    }

    @Transactional(readOnly = true)
    public List<AccountView> accounts(Long tenantId) {
        ensureSilent(tenantId);
        return accounts.findAllByTenantIdOrderByCodeAsc(tenantId).stream().map(this::accountView).toList();
    }

    @Transactional
    public AccountView createAccount(Long tenantId, Long actorId, String code, String name, LedgerAccount.Type type, Long parentId) {
        if (accounts.existsByTenantIdAndCodeIgnoreCase(tenantId, code)) throw conflict("ACCOUNT_CODE_EXISTS", "Account code exists.");
        Tenant tenant = tenants.findById(tenantId).orElseThrow();
        LedgerAccount parent = parentId == null ? null : accounts.findByIdAndTenantId(parentId, tenantId).orElseThrow(() -> notFound("ACCOUNT_NOT_FOUND"));
        LedgerAccount value = accounts.save(new LedgerAccount(tenant, parent, code, name, type));
        audit.record(tenantId, actorId, "LEDGER_ACCOUNT_CREATED", "LEDGER_ACCOUNT", value.getId(), Map.of("code", code));
        return accountView(value);
    }

    @Transactional(readOnly = true)
    public List<TypeView> types(Long tenantId) {
        ensureSilent(tenantId);
        return types.findAllByTenantIdOrderByCodeAsc(tenantId).stream().map(item -> new TypeView(item.getId(), item.getCode(), item.getName(), item.isAutoPost())).toList();
    }

    @Transactional(readOnly = true)
    public List<VoucherView> vouchers(Long tenantId) {
        ensureSilent(tenantId);
        return vouchers.findAllByTenantIdOrderByVoucherDateDescCreatedAtDesc(tenantId).stream().map(this::voucherView).toList();
    }

    @Transactional
    public VoucherView create(Long tenantId, Long actorId, VoucherCommand command) {
        ensureSilent(tenantId);
        Tenant tenant = tenants.findById(tenantId).orElseThrow();
        VoucherType type = types.findByIdAndTenantId(command.voucherTypeId(), tenantId).orElseThrow(() -> notFound("VOUCHER_TYPE_NOT_FOUND"));
        JournalVoucher voucher = new JournalVoucher(tenant, type, uniqueCode(tenantId), command.voucherDate(), command.description(),
                "MANUAL", null, users.findById(actorId).orElseThrow());
        int lineNo = 1;
        for (LineCommand line : command.lines()) {
            LedgerAccount account = accounts.findByIdAndTenantId(line.accountId(), tenantId).orElseThrow(() -> notFound("ACCOUNT_NOT_FOUND"));
            voucher.addLine(account, lineNo++, line.description(), line.debit(), line.credit());
        }
        voucher = vouchers.save(voucher);
        if (command.post()) {
            try { voucher.post(Instant.now(clock)); }
            catch (IllegalStateException ex) { throw conflict(ex.getMessage(), "Voucher is not balanced."); }
        }
        audit.record(tenantId, actorId, "JOURNAL_VOUCHER_CREATED", "JOURNAL_VOUCHER", voucher.getId(), Map.of("code", voucher.getCode()));
        return voucherView(voucher);
    }

    @Transactional
    public VoucherView post(Long tenantId, Long actorId, Long id) {
        JournalVoucher voucher = vouchers.findByIdAndTenantId(id, tenantId).orElseThrow(() -> notFound("VOUCHER_NOT_FOUND"));
        try { voucher.post(Instant.now(clock)); }
        catch (IllegalStateException ex) { throw conflict(ex.getMessage(), "Voucher cannot be posted."); }
        audit.record(tenantId, actorId, "JOURNAL_VOUCHER_POSTED", "JOURNAL_VOUCHER", id, Map.of());
        return voucherView(voucher);
    }

    @Transactional
    public VoucherView voidVoucher(Long tenantId, Long actorId, Long id) {
        JournalVoucher voucher = vouchers.findByIdAndTenantId(id, tenantId).orElseThrow(() -> notFound("VOUCHER_NOT_FOUND"));
        try { voucher.voidPosted(); }
        catch (IllegalStateException ex) { throw conflict(ex.getMessage(), "Voucher cannot be voided."); }
        audit.record(tenantId, actorId, "JOURNAL_VOUCHER_VOIDED", "JOURNAL_VOUCHER", id, Map.of());
        return voucherView(voucher);
    }

    @Transactional
    public void postInvoice(Long tenantId, Invoice invoice) {
        ensureSilent(tenantId);
        if (vouchers.existsByTenantIdAndSourceTypeAndSourceId(tenantId, "INVOICE", invoice.getId())) return;
        BigDecimal total = invoice.getItems().stream().map(item -> item.getCharge().netAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        auto(tenantId, "INV", invoice.getIssuedOn() == null ? LocalDate.now(clock) : invoice.getIssuedOn(),
                "صدور فاکتور " + invoice.getCode(), "INVOICE", invoice.getId(), "1200", total, "4100", total);
    }

    @Transactional
    public void postPayment(Long tenantId, Payment payment) {
        ensureSilent(tenantId);
        if (vouchers.existsByTenantIdAndSourceTypeAndSourceId(tenantId, "PAYMENT", payment.getId())) return;
        auto(tenantId, "PAY", LocalDate.ofInstant(payment.getPaidAt(), java.time.ZoneOffset.UTC),
                "دریافت " + payment.getReceiptNumber(), "PAYMENT", payment.getId(), "1100", payment.getAmount(), "1200", payment.getAmount());
    }

    @Transactional
    public void postRefund(Long tenantId, Payment payment) {
        ensureSilent(tenantId);
        if (vouchers.existsByTenantIdAndSourceTypeAndSourceId(tenantId, "REFUND", payment.getId())) return;
        auto(tenantId, "REF", LocalDate.now(clock), "استرداد " + payment.getReceiptNumber(), "REFUND", payment.getId(),
                "1200", payment.getAmount(), "1100", payment.getAmount());
    }

    @Transactional(readOnly = true)
    public List<TrialRow> trialBalance(Long tenantId) {
        ensureSilent(tenantId);
        Map<Long, TrialRow> rows = new LinkedHashMap<>();
        for (LedgerAccount account : accounts.findAllByTenantIdOrderByCodeAsc(tenantId)) {
            rows.put(account.getId(), new TrialRow(account.getId(), account.getCode(), account.getName(), account.getType().name(), BigDecimal.ZERO, BigDecimal.ZERO));
        }
        for (JournalVoucher voucher : vouchers.findAllByTenantIdOrderByVoucherDateDescCreatedAtDesc(tenantId)) {
            if (voucher.getStatus() != JournalVoucher.Status.POSTED) continue;
            for (JournalVoucherLine line : voucher.getLines()) {
                TrialRow current = rows.get(line.getAccount().getId());
                if (current == null) continue;
                rows.put(line.getAccount().getId(), new TrialRow(current.accountId(), current.code(), current.name(), current.type(),
                        current.debit().add(line.getDebit()), current.credit().add(line.getCredit())));
            }
        }
        return new ArrayList<>(rows.values());
    }

    @Transactional(readOnly = true)
    public String print(Long tenantId, Long id) {
        JournalVoucher voucher = vouchers.findByIdAndTenantId(id, tenantId).orElseThrow(() -> notFound("VOUCHER_NOT_FOUND"));
        StringBuilder body = new StringBuilder("<h1>سند حسابداری ").append(PrintDocuments.escape(voucher.getCode())).append("</h1>");
        body.append("<p class='muted'>").append(PrintDocuments.escape(voucher.getVoucherType().getName())).append(" · ")
                .append(PrintDocuments.day(voucher.getVoucherDate())).append(" · ").append(voucher.getStatus()).append("</p>");
        body.append("<table><thead><tr><th>حساب</th><th>شرح</th><th>بدهکار</th><th>بستانکار</th></tr></thead><tbody>");
        for (JournalVoucherLine line : voucher.getLines()) {
            body.append("<tr><td>").append(PrintDocuments.escape(line.getAccount().getCode() + " " + line.getAccount().getName()))
                    .append("</td><td>").append(PrintDocuments.escape(line.getDescription())).append("</td><td>")
                    .append(PrintDocuments.money(line.getDebit())).append("</td><td>").append(PrintDocuments.money(line.getCredit())).append("</td></tr>");
        }
        body.append("</tbody></table>");
        return PrintDocuments.page("سند " + voucher.getCode(), body.toString(), true);
    }

    private void auto(Long tenantId, String typeCode, LocalDate date, String description, String sourceType, Long sourceId,
            String debitCode, BigDecimal debit, String creditCode, BigDecimal credit) {
        Tenant tenant = tenants.findById(tenantId).orElseThrow();
        VoucherType type = types.findByTenantIdAndCodeIgnoreCase(tenantId, typeCode).orElseThrow();
        LedgerAccount debitAccount = accounts.findByTenantIdAndCodeIgnoreCase(tenantId, debitCode).orElseThrow();
        LedgerAccount creditAccount = accounts.findByTenantIdAndCodeIgnoreCase(tenantId, creditCode).orElseThrow();
        JournalVoucher voucher = new JournalVoucher(tenant, type, uniqueCode(tenantId), date, description, sourceType, sourceId, null);
        voucher.addLine(debitAccount, 1, description, debit, BigDecimal.ZERO);
        voucher.addLine(creditAccount, 2, description, BigDecimal.ZERO, credit);
        voucher.post(Instant.now(clock));
        vouchers.save(voucher);
    }

    private void ensureSilent(Long tenantId) { ensureDefaults(tenantId); }
    private void seedAccount(Tenant tenant, String code, String name, LedgerAccount.Type type) {
        if (!accounts.existsByTenantIdAndCodeIgnoreCase(tenant.getId(), code)) accounts.save(new LedgerAccount(tenant, null, code, name, type));
    }
    private void seedType(Tenant tenant, String code, String name, boolean auto) {
        if (types.findByTenantIdAndCodeIgnoreCase(tenant.getId(), code).isEmpty()) types.save(new VoucherType(tenant, code, name, auto));
    }
    private String uniqueCode(Long tenantId) {
        String code = "JV-" + LocalDate.now(clock).toString().replace("-", "") + "-" + Integer.toHexString(UUID.randomUUID().hashCode() & 0xffff).toUpperCase();
        if (vouchers.existsByTenantIdAndCodeIgnoreCase(tenantId, code)) return uniqueCode(tenantId);
        return code;
    }
    private AccountView accountView(LedgerAccount account) {
        return new AccountView(account.getId(), account.getParent() == null ? null : account.getParent().getId(), account.getCode(),
                account.getName(), account.getType().name(), account.getStatus().name());
    }
    private VoucherView voucherView(JournalVoucher voucher) {
        List<LineView> lineViews = voucher.getLines().stream()
                .map(line -> new LineView(line.getId(), line.getAccount().getId(), line.getAccount().getCode(), line.getAccount().getName(),
                        line.getLineNo(), line.getDescription(), line.getDebit(), line.getCredit()))
                .toList();
        return new VoucherView(voucher.getId(), voucher.getCode(), voucher.getVoucherType().getId(), voucher.getVoucherType().getName(),
                voucher.getVoucherDate(), voucher.getDescription(), voucher.getStatus().name(), voucher.getSourceType(), lineViews);
    }
    private ApiException notFound(String code) { return new ApiException(HttpStatus.NOT_FOUND, code, "Ledger item was not found."); }
    private ApiException conflict(String code, String detail) { return new ApiException(HttpStatus.CONFLICT, code, detail); }

    public record AccountView(Long id, Long parentId, String code, String name, String type, String status) {}
    public record TypeView(Long id, String code, String name, boolean autoPost) {}
    public record LineCommand(Long accountId, String description, BigDecimal debit, BigDecimal credit) {}
    public record VoucherCommand(Long voucherTypeId, LocalDate voucherDate, String description, boolean post, List<LineCommand> lines) {}
    public record LineView(Long id, Long accountId, String accountCode, String accountName, int lineNo, String description, BigDecimal debit, BigDecimal credit) {}
    public record VoucherView(Long id, String code, Long voucherTypeId, String voucherTypeName, LocalDate voucherDate, String description,
            String status, String sourceType, List<LineView> lines) {}
    public record TrialRow(Long accountId, String code, String name, String type, BigDecimal debit, BigDecimal credit) {}
}
