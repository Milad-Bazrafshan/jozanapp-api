package ir.orix.school.reportcard.domain;

import java.math.BigDecimal;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "report_card", schema = "school")
public class ReportCard extends AuditedEntity {
    public enum Status { DRAFT, FINALIZED, PUBLISHED }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "run_id") private ReportCardRun run;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id") private StudentProfile student;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(name = "attendance_percent", precision = 6, scale = 2) private BigDecimal attendancePercent;
    @Column(name = "absence_count") private Integer absenceCount;
    @Column(name = "continuous_score", precision = 8, scale = 2) private BigDecimal continuousScore;
    @Column(name = "rank_in_class") private Integer rankInClass;
    @Column(name = "overall_score", precision = 8, scale = 2) private BigDecimal overallScore;
    @Column(name = "verification_token", nullable = false, length = 80) private String verificationToken;
    @Column(name = "correction_reason", length = 500) private String correctionReason;
    protected ReportCard() {}
    public ReportCard(Tenant tenant, ReportCardRun run, StudentProfile student, String token) {
        this.tenant = tenant; this.run = run; this.student = student; this.verificationToken = token; this.status = Status.DRAFT;
    }
    public void apply(BigDecimal attendancePercent, int absences, BigDecimal overall, Integer rank) {
        this.attendancePercent = attendancePercent; this.absenceCount = absences; this.overallScore = overall; this.rankInClass = rank;
    }
    public void finalizeCard() { status = Status.FINALIZED; }
    public void publish() { status = Status.PUBLISHED; }
    public void correct(String reason) {
        if (status != Status.PUBLISHED) throw new IllegalStateException("NOT_PUBLISHED");
        this.correctionReason = reason; this.status = Status.FINALIZED;
    }
    public ReportCardRun getRun() { return run; } public StudentProfile getStudent() { return student; }
    public Status getStatus() { return status; } public BigDecimal getAttendancePercent() { return attendancePercent; }
    public Integer getAbsenceCount() { return absenceCount; } public BigDecimal getOverallScore() { return overallScore; }
    public Integer getRankInClass() { return rankInClass; } public String getVerificationToken() { return verificationToken; }
    public String getCorrectionReason() { return correctionReason; } public Tenant getTenant() { return tenant; }
}
