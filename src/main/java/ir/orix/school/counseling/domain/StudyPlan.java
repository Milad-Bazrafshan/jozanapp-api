package ir.orix.school.counseling.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "study_plan", schema = "school")
public class StudyPlan extends AuditedEntity {
    public enum Status {ACTIVE, ARCHIVED}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private StudentProfile student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "counselor_membership_id")
    private TenantMembership counselorMembership;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(length = 2000)
    private String description;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @OneToMany(mappedBy = "studyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc")
    private List<StudyPlanItem> items = new ArrayList<>();

    protected StudyPlan() {
    }

    public StudyPlan(Tenant tenant, StudentProfile student, TenantMembership counselor, String title, String description,
                     LocalDate startDate, LocalDate endDate) {
        this.tenant = tenant;
        this.student = student;
        this.counselorMembership = counselor;
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = Status.ACTIVE;
    }

    public void replaceItems(List<StudyPlanItem> values) {
        items.clear();
        int order = 0;
        for (StudyPlanItem item : values) {
            item.bind(this, order++);
            items.add(item);
        }
    }

    public void removeItem(StudyPlanItem item) {
        items.remove(item);
    }

    public void archive() {
        this.status = Status.ARCHIVED;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public StudentProfile getStudent() {
        return student;
    }

    public TenantMembership getCounselorMembership() {
        return counselorMembership;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Status getStatus() {
        return status;
    }

    public List<StudyPlanItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
