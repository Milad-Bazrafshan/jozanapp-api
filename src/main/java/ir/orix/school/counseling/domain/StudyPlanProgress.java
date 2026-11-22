package ir.orix.school.counseling.domain;

import java.time.Instant;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "study_plan_progress", schema = "school")
public class StudyPlanProgress extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "study_plan_item_id") private StudyPlanItem studyPlanItem;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id") private StudentProfile student;
    @Column(name = "completed_minutes", nullable = false) private int completedMinutes;
    @Column(name = "completed_at", nullable = false) private Instant completedAt;
    @Column(length = 500) private String note;

    protected StudyPlanProgress() {}

    public StudyPlanProgress(Tenant tenant, StudyPlanItem item, StudentProfile student, int completedMinutes, Instant completedAt, String note) {
        this.tenant = tenant;
        this.studyPlanItem = item;
        this.student = student;
        this.completedMinutes = completedMinutes;
        this.completedAt = completedAt;
        this.note = note;
    }

    public void update(int completedMinutes, Instant completedAt, String note) {
        this.completedMinutes = completedMinutes;
        this.completedAt = completedAt;
        this.note = note;
    }

    public StudyPlanItem getStudyPlanItem() { return studyPlanItem; }
    public int getCompletedMinutes() { return completedMinutes; }
    public Instant getCompletedAt() { return completedAt; }
    public String getNote() { return note; }
}
