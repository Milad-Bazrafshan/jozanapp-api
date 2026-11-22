package ir.orix.school.counseling.domain;

import java.time.LocalDate;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "counselor_supervision", schema = "school")
public class CounselorSupervision extends AuditedEntity {
    public enum Status {ACTIVE, ENDED}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "senior_membership_id")
    private TenantMembership seniorMembership;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "counselor_membership_id")
    private TenantMembership counselorMembership;
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    protected CounselorSupervision() {
    }

    public CounselorSupervision(Tenant tenant, TenantMembership senior, TenantMembership counselor, LocalDate effectiveFrom) {
        this.tenant = tenant;
        this.seniorMembership = senior;
        this.counselorMembership = counselor;
        this.effectiveFrom = effectiveFrom;
        this.status = Status.ACTIVE;
    }

    public void end(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
        this.status = Status.ENDED;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public TenantMembership getSeniorMembership() {
        return seniorMembership;
    }

    public TenantMembership getCounselorMembership() {
        return counselorMembership;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }
}
