package ir.orix.school.finance.domain;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.*;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "payment", schema = "school")
public class Payment extends AuditedEntity {
    public enum Method { CASH, CARD, BANK_TRANSFER, WALLET, OTHER }
    public enum Provider { INTERNAL, ORIX_WALLET }
    public enum Status { PENDING, SUCCEEDED, FAILED, CANCELLED, REFUNDED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id") private StudentProfile student;
    @Column(name = "receipt_number", nullable = false, length = 60) private String receiptNumber;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private Method method;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private Provider provider;
    @Column(name = "external_reference", length = 120) private String externalReference;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(name = "paid_at", nullable = false) private Instant paidAt;
    @Column(length = 500) private String notes;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "recorded_by") private UserAccount recordedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "refunded_payment_id") private Payment refundedPayment;
    @Column(name = "refund_reason", length = 500) private String refundReason;

    protected Payment() {}

    public Payment(Tenant tenant, StudentProfile student, String receiptNumber, BigDecimal amount, Method method,
            Provider provider, String externalReference, Instant paidAt, String notes, UserAccount recordedBy) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("INVALID_PAYMENT_AMOUNT");
        if (provider == Provider.ORIX_WALLET) {
            throw new IllegalStateException("WALLET_NOT_ENABLED");
        }
        this.tenant = tenant;
        this.student = student;
        this.receiptNumber = receiptNumber.strip().toUpperCase();
        this.amount = amount;
        this.method = method;
        this.provider = provider == null ? Provider.INTERNAL : provider;
        this.externalReference = externalReference == null || externalReference.isBlank() ? null : externalReference.strip();
        this.paidAt = paidAt;
        this.notes = notes == null || notes.isBlank() ? null : notes.strip();
        this.recordedBy = recordedBy;
        this.status = Status.SUCCEEDED;
    }

    public void ensureAllocatable() {
        if (status != Status.SUCCEEDED) throw new IllegalStateException("PAYMENT_NOT_SUCCEEDED");
    }

    public void markRefunded(String reason) {
        if (status != Status.SUCCEEDED) throw new IllegalStateException("PAYMENT_NOT_REFUNDABLE");
        this.status = Status.REFUNDED;
        this.refundReason = reason == null || reason.isBlank() ? null : reason.strip();
    }

    public Tenant getTenant() { return tenant; }
    public StudentProfile getStudent() { return student; }
    public String getReceiptNumber() { return receiptNumber; }
    public BigDecimal getAmount() { return amount; }
    public Method getMethod() { return method; }
    public Provider getProvider() { return provider; }
    public String getExternalReference() { return externalReference; }
    public Status getStatus() { return status; }
    public Instant getPaidAt() { return paidAt; }
    public String getNotes() { return notes; }
    public UserAccount getRecordedBy() { return recordedBy; }
    public Payment getRefundedPayment() { return refundedPayment; }
    public String getRefundReason() { return refundReason; }
}
