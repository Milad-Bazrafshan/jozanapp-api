package ir.orix.school.people.domain;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "instructor_profile", schema = "school")
public class InstructorProfile extends AuditedEntity {
    public enum Status { ACTIVE, INACTIVE, SUSPENDED }
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="tenant_id") private Tenant tenant;
    @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="person_id") private Person person;
    @Column(name="instructor_code", nullable=false, length=50) private String instructorCode;
    @Column(length=120) private String title;
    @Column(length=300) private String specialization;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
    protected InstructorProfile(){}
    public InstructorProfile(Tenant tenant, Person person, String code, String title, String specialization){this.tenant=tenant;this.person=person;this.instructorCode=code.strip().toUpperCase();this.title=normalize(title);this.specialization=normalize(specialization);this.status=Status.ACTIVE;}
    private static String normalize(String v){return v==null||v.isBlank()?null:v.strip();}
    public Person getPerson(){return person;} public String getInstructorCode(){return instructorCode;} public String getTitle(){return title;} public String getSpecialization(){return specialization;} public Status getStatus(){return status;}
}
