package ir.orix.school.counseling.domain;

import java.time.Instant;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "counselor_admin_report", schema = "school")
public class CounselorAdminReport extends AuditedEntity {
    public enum Status {DRAFT, SENT}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "counselor_membership_id")
    private TenantMembership counselorMembership;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;
    @Column(name = "period_label", length = 120)
    private String periodLabel;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;
    @Column(name = "sent_at")
    private Instant sentAt;

    protected CounselorAdminReport() {
    }

    public CounselorAdminReport(Tenant tenant, TenantMembership counselor, String title, String body, String periodLabel) {
        this.tenant = tenant;
        this.counselorMembership = counselor;
        this.title = title;
        this.body = body;
        this.periodLabel = periodLabel;
        this.status = Status.DRAFT;
    }

    public void send(Instant at) {
        this.status = Status.SENT;
        this.sentAt = at;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public TenantMembership getCounselorMembership() {
        return counselorMembership;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
