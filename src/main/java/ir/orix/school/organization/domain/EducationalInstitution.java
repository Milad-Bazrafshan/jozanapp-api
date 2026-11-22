package ir.orix.school.organization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "educational_institution", schema = "school")
public class EducationalInstitution extends AuditedEntity {

    public enum Type { SCHOOL, LANGUAGE_INSTITUTE, EXAM_PREPARATION, VOCATIONAL, GENERAL }
    public enum Status { ACTIVE, INACTIVE }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "institution_type", nullable = false, length = 40)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    protected EducationalInstitution() {
    }

    public EducationalInstitution(Tenant tenant, String name, Type type) {
        this.tenant = tenant;
        this.name = name.strip();
        this.type = type;
        this.status = Status.ACTIVE;
    }

    public Tenant getTenant() { return tenant; }
    public String getName() { return name; }
    public Type getType() { return type; }
    public Status getStatus() { return status; }
}
