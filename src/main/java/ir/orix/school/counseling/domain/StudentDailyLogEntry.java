package ir.orix.school.counseling.domain;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "student_daily_log_entry", schema = "school")
public class StudentDailyLogEntry extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "daily_log_id") private StudentDailyLog dailyLog;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(name = "activity_title", nullable = false, length = 200) private String activityTitle;
    @Column(name = "subject_name", length = 120) private String subjectName;
    @Column(name = "duration_minutes", nullable = false) private int durationMinutes;
    @Column(length = 1000) private String notes;

    protected StudentDailyLogEntry() {}

    public StudentDailyLogEntry(Tenant tenant, String activityTitle, String subjectName, int durationMinutes, String notes) {
        this.tenant = tenant;
        this.activityTitle = activityTitle;
        this.subjectName = subjectName;
        this.durationMinutes = durationMinutes;
        this.notes = notes;
    }

    void bind(StudentDailyLog log, int sortOrder) {
        this.dailyLog = log;
        this.tenant = log.getTenant();
        this.sortOrder = sortOrder;
    }

    public String getActivityTitle() { return activityTitle; }
    public String getSubjectName() { return subjectName; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getNotes() { return notes; }
    public int getSortOrder() { return sortOrder; }
}
