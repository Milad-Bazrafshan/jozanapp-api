package ir.orix.school.organization.domain;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import ir.orix.school.identity.domain.UserAccount;

@Entity
@Table(name = "access_grant", schema = "school")
public class AccessGrant {

    public enum ScopeType { TENANT, BRANCH }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id", nullable = false)
    private TenantMembership membership;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private AccessRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    private ScopeType scopeType;

    @Column(name = "scope_id")
    private Long scopeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    protected AccessGrant() {
    }

    public AccessGrant(Tenant tenant, TenantMembership membership, AccessRole role, ScopeType scopeType, Long scopeId, UserAccount createdBy) {
        this();
        this.tenant = tenant;
        this.membership = membership;
        this.role = role;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public TenantMembership getMembership() { return membership; }
    public AccessRole getRole() { return role; }
    public ScopeType getScopeType() { return scopeType; }
    public Long getScopeId() { return scopeId; }
}
