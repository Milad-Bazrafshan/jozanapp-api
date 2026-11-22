package ir.orix.school.academic.domain;

import java.time.LocalDate;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name="academic_period",schema="school")
public class AcademicPeriod extends AuditedEntity {
    public enum Type { ACADEMIC_YEAR, TERM, SEMESTER, QUARTER, CUSTOM }
    public enum Status { DRAFT, ACTIVE, CLOSED }
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant;
    @Column(nullable=false,length=50) private String code; @Column(nullable=false,length=150) private String name;
    @Enumerated(EnumType.STRING) @Column(name="period_type",nullable=false,length=30) private Type type;
    @Column(name="starts_on",nullable=false) private LocalDate startsOn; @Column(name="ends_on",nullable=false) private LocalDate endsOn;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
    protected AcademicPeriod(){}
    public AcademicPeriod(Tenant tenant,String code,String name,Type type,LocalDate startsOn,LocalDate endsOn){this.tenant=tenant;this.code=code.strip().toUpperCase();this.name=name.strip();this.type=type;this.startsOn=startsOn;this.endsOn=endsOn;this.status=Status.DRAFT;}
    public Tenant getTenant(){return tenant;} public String getCode(){return code;} public String getName(){return name;} public Type getType(){return type;} public LocalDate getStartsOn(){return startsOn;} public LocalDate getEndsOn(){return endsOn;} public Status getStatus(){return status;}
}
