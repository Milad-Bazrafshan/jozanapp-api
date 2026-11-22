package ir.orix.school.enrollment.domain;

import java.time.LocalDate; import jakarta.persistence.*; import ir.orix.school.classroom.domain.ClassSection; import ir.orix.school.identity.domain.UserAccount; import ir.orix.school.organization.domain.Tenant; import ir.orix.school.people.domain.StudentProfile; import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name="enrollment",schema="school")
public class Enrollment extends AuditedEntity {
 public enum Status{ACTIVE,WAITLISTED,SUSPENDED,COMPLETED,CANCELLED,TRANSFERRED,DROPPED} public enum Source{ADMIN,IMPORT,SELF_SERVICE,TRANSFER}
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="class_section_id") private ClassSection classSection; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="student_id") private StudentProfile student;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status; @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Source source; @Column(name="started_on",nullable=false) private LocalDate startedOn; @Column(name="ended_on") private LocalDate endedOn; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="enrolled_by") private UserAccount enrolledBy;
 protected Enrollment(){}
 public Enrollment(Tenant tenant,ClassSection section,StudentProfile student,Status status,Source source,LocalDate startedOn,UserAccount actor){this.tenant=tenant;this.classSection=section;this.student=student;this.status=status;this.source=source;this.startedOn=startedOn;this.enrolledBy=actor;}
 public ClassSection getClassSection(){return classSection;} public StudentProfile getStudent(){return student;} public Status getStatus(){return status;} public Source getSource(){return source;} public LocalDate getStartedOn(){return startedOn;} public LocalDate getEndedOn(){return endedOn;}
 public Status changeStatus(Status next,LocalDate endedOn){Status previous=status;status=next;this.endedOn=endedOn;return previous;}
}
