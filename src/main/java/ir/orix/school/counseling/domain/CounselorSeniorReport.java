package ir.orix.school.counseling.domain;

import java.time.Instant;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "counselor_senior_report", schema = "school")
public class CounselorSeniorReport extends AuditedEntity {
    public enum Status {DRAFT, PENDING_REVIEW, SENT}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "counselor_membership_id")
    private TenantMembership counselorMembership;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "senior_membership_id")
    private TenantMembership seniorMembership;
    @Column(name = "report_year", nullable = false)
    private int reportYear;
    @Column(name = "report_month", nullable = false)
    private int reportMonth;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;
    @Column(name = "ai_analysis", columnDefinition = "TEXT")
    private String aiAnalysis;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;
    @Column(name = "finalized_at")
    private Instant finalizedAt;

    protected CounselorSeniorReport() {
    }

    public CounselorSeniorReport(Tenant tenant, TenantMembership counselor, TenantMembership senior, int year, int month,
                                 String title, String body) {
        this.tenant = tenant;
        this.counselorMembership = counselor;
        this.seniorMembership = senior;
        this.reportYear = year;
        this.reportMonth = month;
        this.title = title;
        this.body = body;
        this.status = Status.PENDING_REVIEW;
    }

    public void applyAiAnalysis(String analysis) {
        this.aiAnalysis = analysis;
    }

    public void finalizeReport(Instant at) {
        this.status = Status.SENT;
        this.finalizedAt = at;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public TenantMembership getCounselorMembership() {
        return counselorMembership;
    }

    public TenantMembership getSeniorMembership() {
        return seniorMembership;
    }

    public int getReportYear() {
        return reportYear;
    }

    public int getReportMonth() {
        return reportMonth;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getAiAnalysis() {
        return aiAnalysis;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getFinalizedAt(){
        return finalizedAt;
    }
}
