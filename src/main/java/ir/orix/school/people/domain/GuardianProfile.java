package ir.orix.school.people.domain;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name="guardian_profile",schema="school")
public class GuardianProfile extends AuditedEntity {
    public enum Status { ACTIVE, INACTIVE }
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="person_id") private Person person;
    @Column(length=150) private String occupation;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
    protected GuardianProfile(){}
    public GuardianProfile(Tenant tenant,Person person,String occupation){this.tenant=tenant;this.person=person;this.occupation=occupation==null||occupation.isBlank()?null:occupation.strip();this.status=Status.ACTIVE;}
    public Person getPerson(){return person;} public String getOccupation(){return occupation;} public Status getStatus(){return status;}
}
