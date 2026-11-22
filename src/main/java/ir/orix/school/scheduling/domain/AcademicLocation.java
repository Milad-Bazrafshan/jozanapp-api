package ir.orix.school.scheduling.domain;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.*;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "academic_location", schema = "school")
public class AcademicLocation extends AuditedEntity {
    public enum Type {CLASSROOM, LAB, HALL, ONLINE}

    public enum Status {ACTIVE, INACTIVE}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private Branch branch;
    @Column(nullable = false, length = 60)
    private String code;
    @Column(nullable = false, length = 160)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 30)
    private Type type;
    private Integer capacity;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    protected AcademicLocation() {
    }

    public AcademicLocation(Tenant tenant, Branch branch, String code, String name, Type type, Integer capacity) {
        this.tenant = tenant;
        this.branch = branch;
        this.code = code.strip().toUpperCase();
        this.name = name.strip();
        this.type = type;
        this.capacity = capacity;
        this.status = Status.ACTIVE;
    }

    public Branch getBranch() {
        return branch;
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

    public Integer getCapacity() {
        return capacity;
    }

    public Status getStatus() {
        return status;
    }
}
