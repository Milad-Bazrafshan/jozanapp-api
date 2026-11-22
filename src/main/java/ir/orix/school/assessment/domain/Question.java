package ir.orix.school.assessment.domain;

import jakarta.persistence.*;
import ir.orix.school.academic.domain.Course;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;
import lombok.Data;

@Data
@Entity
@Table(name = "question", schema = "school")
public class Question extends AuditedEntity {
    public enum Type {SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER, ESSAY}

    public enum Status {ACTIVE, ARCHIVED}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;
    @Column(nullable = false, length = 60)
    private String code;
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private Type type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;
    @Column(name = "current_version_number", nullable = false)
    private int currentVersionNumber;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    protected Question() {
    }

    public Question(Tenant tenant, Course course, String code, Type type, UserAccount actor) {
        this.tenant = tenant;
        this.course = course;
        this.code = code.strip().toUpperCase();
        this.type = type;
        this.createdBy = actor;
        this.status = Status.ACTIVE;
        this.currentVersionNumber = 1;
    }

    public int nextVersion() {
        return ++currentVersionNumber;
    }
}
