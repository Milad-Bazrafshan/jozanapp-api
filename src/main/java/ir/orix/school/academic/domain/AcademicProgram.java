package ir.orix.school.academic.domain;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "academic_program", schema = "school")
public class AcademicProgram extends AuditedEntity {
    public enum Type {SCHOOL_GRADE, LANGUAGE_LEVEL, EXAM_PREPARATION, VOCATIONAL_SKILL, GENERAL}

    public enum Status {ACTIVE, INACTIVE}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    @Column(nullable = false, length = 50)
    private String code;
    @Column(nullable = false, length = 180)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "program_type", nullable = false, length = 40)
    private Type type;
    @Column(length = 700)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    protected AcademicProgram() {
    }

    public AcademicProgram(Tenant tenant, String code, String name, Type type, String description) {
        this.tenant = tenant;
        this.code = code.strip().toUpperCase();
        this.name = name.strip();
        this.type = type;
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.status = Status.ACTIVE;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }
}
