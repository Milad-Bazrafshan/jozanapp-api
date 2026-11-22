package ir.orix.school.finance.domain;

import java.math.BigDecimal;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "payment_allocation", schema = "school")
public class PaymentAllocation extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "payment_id") private Payment payment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "charge_id") private Charge charge;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;

    protected PaymentAllocation() {}

    public PaymentAllocation(Tenant tenant, Payment payment, Charge charge, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("INVALID_ALLOCATION_AMOUNT");
        if (!payment.getStudent().getId().equals(charge.getStudent().getId())) {
            throw new IllegalStateException("ALLOCATION_STUDENT_MISMATCH");
        }
        payment.ensureAllocatable();
        if (charge.getStatus() == Charge.Status.VOID || charge.getStatus() == Charge.Status.PAID) {
            throw new IllegalStateException("CHARGE_NOT_ALLOCATABLE");
        }
        this.tenant = tenant;
        this.payment = payment;
        this.charge = charge;
        this.amount = amount;
    }

    public Payment getPayment() { return payment; }
    public Charge getCharge() { return charge; }
    public BigDecimal getAmount() { return amount; }
}
