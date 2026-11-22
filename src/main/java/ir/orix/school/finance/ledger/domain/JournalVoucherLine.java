package ir.orix.school.finance.ledger.domain;

import java.math.BigDecimal;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "journal_voucher_line", schema = "school")
public class JournalVoucherLine extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "voucher_id") private JournalVoucher voucher;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id") private LedgerAccount account;
    @Column(name = "line_no", nullable = false) private int lineNo;
    @Column(length = 400) private String description;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal debit;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal credit;
    protected JournalVoucherLine() {}
    public JournalVoucherLine(Tenant tenant, JournalVoucher voucher, LedgerAccount account, int lineNo, String description, BigDecimal debit, BigDecimal credit) {
        this.tenant = tenant; this.voucher = voucher; this.account = account; this.lineNo = lineNo; this.description = description;
        this.debit = debit == null ? BigDecimal.ZERO : debit; this.credit = credit == null ? BigDecimal.ZERO : credit;
        if (this.debit.signum() < 0 || this.credit.signum() < 0 || (this.debit.signum() > 0 && this.credit.signum() > 0) || (this.debit.signum() == 0 && this.credit.signum() == 0)) {
            throw new IllegalArgumentException("INVALID_VOUCHER_LINE");
        }
    }
    public LedgerAccount getAccount() { return account; } public int getLineNo() { return lineNo; }
    public String getDescription() { return description; } public BigDecimal getDebit() { return debit; } public BigDecimal getCredit() { return credit; }
}
