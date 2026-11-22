package ir.orix.school.finance.domain;

import java.math.BigDecimal;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "guardian_wallet_txn", schema = "school")
public class WalletTransaction extends AuditedEntity {
    public enum Type { TOP_UP, PAYMENT }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "wallet_id") private Wallet wallet;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id") private StudentProfile student;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_id") private Payment payment;
    @Enumerated(EnumType.STRING) @Column(name = "txn_type", nullable = false, length = 30) private Type type;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(name = "balance_after", nullable = false, precision = 14, scale = 2) private BigDecimal balanceAfter;
    @Column(length = 500) private String notes;

    protected WalletTransaction() {}

    public WalletTransaction(Tenant tenant, Wallet wallet, Type type, BigDecimal amount, BigDecimal balanceAfter,
            StudentProfile student, Payment payment, String notes) {
        this.tenant = tenant;
        this.wallet = wallet;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.student = student;
        this.payment = payment;
        this.notes = notes == null || notes.isBlank() ? null : notes.strip();
    }

    public Type getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public StudentProfile getStudent() { return student; }
    public Payment getPayment() { return payment; }
    public String getNotes() { return notes; }
}
