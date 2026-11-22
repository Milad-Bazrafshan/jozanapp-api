package ir.orix.school.reportcard.domain;

import java.time.Instant;
import jakarta.persistence.*;
import ir.orix.school.academic.domain.AcademicPeriod;
import ir.orix.school.classroom.domain.ClassSection;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "report_card_run", schema = "school")
public class ReportCardRun extends AuditedEntity {
    public enum Status { DRAFT, FINALIZED, PUBLISHED }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "academic_period_id") private AcademicPeriod period;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "class_section_id") private ClassSection classSection;
    @Column(nullable = false, length = 200) private String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(name = "include_rank", nullable = false) private boolean includeRank;
    @Column(name = "published_at") private Instant publishedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by") private UserAccount createdBy;
    protected ReportCardRun() {}
    public ReportCardRun(Tenant tenant, AcademicPeriod period, ClassSection section, String title, boolean includeRank, UserAccount actor) {
        this.tenant = tenant; this.period = period; this.classSection = section; this.title = title.strip(); this.includeRank = includeRank;
        this.createdBy = actor; this.status = Status.DRAFT;
    }
    public void finalizeRun() { if (status != Status.DRAFT) throw new IllegalStateException("RUN_NOT_DRAFT"); status = Status.FINALIZED; }
    public void publish(Instant now) { if (status != Status.FINALIZED && status != Status.PUBLISHED) throw new IllegalStateException("RUN_NOT_FINALIZED"); status = Status.PUBLISHED; publishedAt = now; }
    public Tenant getTenant() { return tenant; } public AcademicPeriod getPeriod() { return period; }
    public ClassSection getClassSection() { return classSection; } public String getTitle() { return title; }
    public Status getStatus() { return status; } public boolean isIncludeRank() { return includeRank; }
    public Instant getPublishedAt() { return publishedAt; }
}
