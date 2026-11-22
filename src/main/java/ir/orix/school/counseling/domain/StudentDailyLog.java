package ir.orix.school.counseling.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "student_daily_log", schema = "school")
public class StudentDailyLog extends AuditedEntity {
    public enum Status { DRAFT, SUBMITTED, REVIEWED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id") private StudentProfile student;
    @Column(name = "log_date", nullable = false) private LocalDate logDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(name = "day_summary", length = 2000) private String daySummary;
    @Column(name = "counselor_feedback", length = 2000) private String counselorFeedback;
    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(name = "reviewed_by_membership_id") private Long reviewedByMembershipId;

    @OneToMany(mappedBy = "dailyLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc")
    private List<StudentDailyLogEntry> entries = new ArrayList<>();

    protected StudentDailyLog() {}

    public StudentDailyLog(Tenant tenant, StudentProfile student, LocalDate logDate) {
        this.tenant = tenant;
        this.student = student;
        this.logDate = logDate;
        this.status = Status.DRAFT;
    }

    public void replaceEntries(List<StudentDailyLogEntry> values) {
        entries.clear();
        int order = 0;
        for (StudentDailyLogEntry entry : values) {
            entry.bind(this, order++);
            entries.add(entry);
        }
    }

    public void submit() {
        if (status == Status.REVIEWED) {
            throw new IllegalStateException("Already reviewed");
        }
        this.status = Status.SUBMITTED;
    }

    public void review(String feedback, Long reviewerMembershipId, Instant at) {
        this.counselorFeedback = feedback;
        this.reviewedByMembershipId = reviewerMembershipId;
        this.reviewedAt = at;
        this.status = Status.REVIEWED;
    }

    public Tenant getTenant() { return tenant; }
    public StudentProfile getStudent() { return student; }
    public LocalDate getLogDate() { return logDate; }
    public Status getStatus() { return status; }
    public String getDaySummary() { return daySummary; }
    public void setDaySummary(String daySummary) { this.daySummary = daySummary; }
    public String getCounselorFeedback() { return counselorFeedback; }
    public Instant getReviewedAt() { return reviewedAt; }
    public List<StudentDailyLogEntry> getEntries() { return Collections.unmodifiableList(entries); }
}
