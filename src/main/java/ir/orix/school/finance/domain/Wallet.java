package ir.orix.school.finance.domain;

import java.math.BigDecimal;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.people.domain.GuardianProfile;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "guardian_wallet", schema = "school")
public class Wallet extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "guardian_id") private GuardianProfile guardian;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal balance;

    protected Wallet() {}

    public Wallet(Tenant tenant, GuardianProfile guardian) {
        this.tenant = tenant;
        this.guardian = guardian;
        this.balance = BigDecimal.ZERO.setScale(2);
    }

    public BigDecimal credit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(amount);
        return balance;
    }

    public BigDecimal debit(BigDecimal amount) {
        requirePositive(amount);
        if (balance.compareTo(amount) < 0) throw new IllegalStateException("INSUFFICIENT_WALLET");
        balance = balance.subtract(amount);
        return balance;
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("INVALID_WALLET_AMOUNT");
    }

    public Tenant getTenant() { return tenant; }
    public GuardianProfile getGuardian() { return guardian; }
    public BigDecimal getBalance() { return balance; }
}
