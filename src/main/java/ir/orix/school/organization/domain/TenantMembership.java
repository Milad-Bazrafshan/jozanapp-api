package ir.orix.school.organization.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.people.domain.Person;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "tenant_membership", schema = "school")
public class TenantMembership extends AuditedEntity {

    public enum Status { ACTIVE, SUSPENDED, REVOKED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected TenantMembership() {
    }

    public TenantMembership(Tenant tenant, UserAccount userAccount, Person person, Instant now) {
        this.tenant = tenant;
        this.userAccount = userAccount;
        this.person = person;
        this.joinedAt = now;
        this.status = Status.ACTIVE;
    }

    public void changeStatus(Status status) { this.status = status; }

    public Tenant getTenant() { return tenant; }
    public UserAccount getUserAccount() { return userAccount; }
    public Person getPerson() { return person; }
    public Status getStatus() { return status; }
}
