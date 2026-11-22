package ir.orix.school.finance.ledger.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "journal_voucher", schema = "school")
public class JournalVoucher extends AuditedEntity {
    public enum Status { DRAFT, POSTED, VOID }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "voucher_type_id") private VoucherType voucherType;
    @Column(nullable = false, length = 60) private String code;
    @Column(name = "voucher_date", nullable = false) private LocalDate voucherDate;
    @Column(length = 500) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(name = "source_type", length = 40) private String sourceType;
    @Column(name = "source_id") private Long sourceId;
    @Column(name = "posted_at") private Instant postedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by") private UserAccount createdBy;
    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("lineNo")
    private List<JournalVoucherLine> lines = new ArrayList<>();
    protected JournalVoucher() {}
    public JournalVoucher(Tenant tenant, VoucherType type, String code, LocalDate date, String description, String sourceType, Long sourceId, UserAccount actor) {
        this.tenant = tenant; this.voucherType = type; this.code = code; this.voucherDate = date; this.description = description;
        this.sourceType = sourceType; this.sourceId = sourceId; this.createdBy = actor; this.status = Status.DRAFT;
    }
    public JournalVoucherLine addLine(LedgerAccount account, int lineNo, String description, BigDecimal debit, BigDecimal credit) {
        JournalVoucherLine line = new JournalVoucherLine(tenant, this, account, lineNo, description, debit, credit);
        lines.add(line);
        return line;
    }
    public void post(Instant now) {
        if (status != Status.DRAFT) throw new IllegalStateException("VOUCHER_NOT_DRAFT");
        BigDecimal debit = lines.stream().map(JournalVoucherLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = lines.stream().map(JournalVoucherLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (debit.compareTo(credit) != 0 || debit.signum() == 0) throw new IllegalStateException("VOUCHER_UNBALANCED");
        status = Status.POSTED; postedAt = now;
    }
    public void voidPosted() { if (status != Status.POSTED) throw new IllegalStateException("VOUCHER_NOT_POSTED"); status = Status.VOID; }
    public Tenant getTenant() { return tenant; } public VoucherType getVoucherType() { return voucherType; }
    public String getCode() { return code; } public LocalDate getVoucherDate() { return voucherDate; }
    public String getDescription() { return description; } public Status getStatus() { return status; }
    public String getSourceType() { return sourceType; } public Long getSourceId() { return sourceId; }
    public Instant getPostedAt() { return postedAt; } public List<JournalVoucherLine> getLines() { return lines; }
}
