package ir.orix.school.identity.domain;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.domain.TenantMembership;

@Entity
@Table(name = "refresh_token", schema = "school")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id", nullable = false)
    private TenantMembership membership;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_id")
    private Long replacedById;

    protected RefreshToken() {
    }

    public RefreshToken(String tokenHash, UserAccount userAccount, TenantMembership membership, Tenant tenant, Instant expiresAt) {
        this();
        this.tokenHash = tokenHash;
        this.userAccount = userAccount;
        this.membership = membership;
        this.tenant = tenant;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public boolean isUsableAt(Instant now) { return revokedAt == null && expiresAt.isAfter(now); }
    public void revoke(Instant now, Long replacementId) { revokedAt = now; replacedById = replacementId; }

    public Long getId() { return id; }
    public UserAccount getUserAccount() { return userAccount; }
    public TenantMembership getMembership() { return membership; }
    public Tenant getTenant() { return tenant; }
}
