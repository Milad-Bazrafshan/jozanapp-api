package ir.orix.school.classroom.domain;

import java.time.LocalDate; import jakarta.persistence.*; import ir.orix.school.organization.domain.Tenant; import ir.orix.school.people.domain.InstructorProfile; import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name="class_instructor",schema="school")
public class ClassInstructor extends AuditedEntity {
 public enum Role{PRIMARY_INSTRUCTOR,ASSISTANT_INSTRUCTOR,SUBSTITUTE_INSTRUCTOR,GUEST_INSTRUCTOR,EXAMINER} public enum Status{ACTIVE,INACTIVE}
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="class_section_id") private ClassSection classSection; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="instructor_id") private InstructorProfile instructor;
 @Enumerated(EnumType.STRING) @Column(name="instructor_role",nullable=false,length=40) private Role role; @Column(name="is_primary",nullable=false) private boolean primary; @Column(name="effective_from",nullable=false) private LocalDate effectiveFrom; @Column(name="effective_to") private LocalDate effectiveTo; @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
 protected ClassInstructor(){}
 public ClassInstructor(Tenant tenant,ClassSection section,InstructorProfile instructor,Role role,boolean primary,LocalDate from,LocalDate to){this.tenant=tenant;this.classSection=section;this.instructor=instructor;this.role=role;this.primary=primary;this.effectiveFrom=from;this.effectiveTo=to;this.status=Status.ACTIVE;}
 public ClassSection getClassSection(){return classSection;} public InstructorProfile getInstructor(){return instructor;} public Role getRole(){return role;} public boolean isPrimary(){return primary;} public LocalDate getEffectiveFrom(){return effectiveFrom;} public LocalDate getEffectiveTo(){return effectiveTo;} public Status getStatus(){return status;}
}
