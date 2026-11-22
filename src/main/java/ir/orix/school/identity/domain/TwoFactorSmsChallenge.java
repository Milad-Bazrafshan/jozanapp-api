package ir.orix.school.identity.domain;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "two_factor_sms_challenge", schema = "school")
public class TwoFactorSmsChallenge extends AuditedEntity {

    public enum Purpose {
        LOGIN,
        ENABLE_2FA,
        DISABLE_2FA
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private UserAccount account;

    @Column(name = "purpose", nullable = false, length = 30)
    private String purpose;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean consumed;

    protected TwoFactorSmsChallenge() {
    }

    public TwoFactorSmsChallenge(UserAccount account, Purpose purpose, String codeHash, Instant expiresAt) {
        this.account = account;
        this.purpose = purpose.name();
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.consumed = false;
    }

    public UserAccount getAccount() {
        return account;
    }

    public Purpose purposeEnum() {
        return Purpose.valueOf(purpose);
    }

    public boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void consume() {
        this.consumed = true;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public Long getAccountId() {
        return account.getId();
    }

    public String getCodeHash() {
        return codeHash;
    }
}
