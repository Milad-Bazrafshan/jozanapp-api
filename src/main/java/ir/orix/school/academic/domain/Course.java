package ir.orix.school.academic.domain;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name="course",schema="school")
public class Course extends AuditedEntity {
    public enum Status { ACTIVE, INACTIVE }
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant;
    @Column(nullable=false,length=50) private String code; @Column(nullable=false,length=200) private String title;
    @Column(length=1000) private String description; @Column(name="duration_minutes") private Integer durationMinutes;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
    protected Course(){}
    public Course(Tenant tenant,String code,String title,String description,Integer durationMinutes){this.tenant=tenant;this.code=code.strip().toUpperCase();this.title=title.strip();this.description=description==null||description.isBlank()?null:description.strip();this.durationMinutes=durationMinutes;this.status=Status.ACTIVE;}
    public Tenant getTenant(){return tenant;} public String getCode(){return code;} public String getTitle(){return title;} public String getDescription(){return description;} public Integer getDurationMinutes(){return durationMinutes;} public Status getStatus(){return status;}
}
