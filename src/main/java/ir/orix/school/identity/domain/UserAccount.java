package ir.orix.school.identity.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "user_account", schema = "school")
public class UserAccount extends AuditedEntity {

    public enum Status { ACTIVE, BLOCKED, DISABLED }

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "two_factor_sms_enabled", nullable = true)
    private Boolean twoFactorSmsEnabled;

    protected UserAccount() {
    }

    public UserAccount(String email, String passwordHash, Instant now) {
        this.email = email.strip().toLowerCase();
        this.passwordHash = passwordHash;
        this.passwordChangedAt = now;
        this.status = Status.ACTIVE;
        this.twoFactorSmsEnabled = false;
    }

    public void recordLogin(Instant now) { this.lastLoginAt = now; }
    public void changePassword(String passwordHash, Instant now) {
        this.passwordHash = passwordHash;
        this.passwordChangedAt = now;
    }
    public void changeStatus(Status status) { this.status = status; }

    public boolean isTwoFactorSmsEnabled() {
        return twoFactorSmsEnabled != null ? twoFactorSmsEnabled : false;
    }

    public void setTwoFactorSmsEnabled(boolean enabled) {
        this.twoFactorSmsEnabled = enabled;
    }

    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Status getStatus() { return status; }
    public Instant getLastLoginAt() { return lastLoginAt; }
}
