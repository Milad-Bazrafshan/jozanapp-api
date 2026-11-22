package ir.orix.school.counseling.domain;

import java.time.LocalDate;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "study_plan_item", schema = "school")
public class StudyPlanItem extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "study_plan_id") private StudyPlan studyPlan;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(name = "subject_name", nullable = false, length = 120) private String subjectName;
    @Column(name = "activity_title", nullable = false, length = 200) private String activityTitle;
    @Column(name = "target_minutes", nullable = false) private int targetMinutes;
    @Column(name = "planned_date") private LocalDate plannedDate;
    @Column(length = 1000) private String notes;

    protected StudyPlanItem() {}

    public StudyPlanItem(Tenant tenant, String subjectName, String activityTitle, int targetMinutes, LocalDate plannedDate, String notes) {
        this.tenant = tenant;
        this.subjectName = subjectName;
        this.activityTitle = activityTitle;
        this.targetMinutes = targetMinutes;
        this.plannedDate = plannedDate;
        this.notes = notes;
    }

    void bind(StudyPlan plan, int sortOrder) {
        this.studyPlan = plan;
        this.tenant = plan.getTenant();
        this.sortOrder = sortOrder;
    }

    public StudyPlan getStudyPlan() { return studyPlan; }
    public int getSortOrder() { return sortOrder; }
    public String getSubjectName() { return subjectName; }
    public String getActivityTitle() { return activityTitle; }
    public int getTargetMinutes() { return targetMinutes; }
    public LocalDate getPlannedDate() { return plannedDate; }
    public String getNotes() { return notes; }
}
