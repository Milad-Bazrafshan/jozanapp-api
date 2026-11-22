package ir.orix.school.classroom.domain;

import java.time.LocalDate; import jakarta.persistence.*;
import ir.orix.school.academic.domain.*; import ir.orix.school.organization.domain.*; import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name="course_offering",schema="school")
public class CourseOffering extends AuditedEntity {
 public enum Status{DRAFT,OPEN,CLOSED,CANCELLED}
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="academic_period_id") private AcademicPeriod academicPeriod;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="course_id") private Course course;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="branch_id") private Branch branch;
 @Column(nullable=false,length=60) private String code; @Column(name="enrollment_starts_on") private LocalDate enrollmentStartsOn; @Column(name="enrollment_ends_on") private LocalDate enrollmentEndsOn;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
 protected CourseOffering(){}
 public CourseOffering(Tenant tenant,AcademicPeriod period,Course course,Branch branch,String code,LocalDate starts,LocalDate ends){this.tenant=tenant;this.academicPeriod=period;this.course=course;this.branch=branch;this.code=code.strip().toUpperCase();this.enrollmentStartsOn=starts;this.enrollmentEndsOn=ends;this.status=Status.OPEN;}
 public Tenant getTenant(){return tenant;} public AcademicPeriod getAcademicPeriod(){return academicPeriod;} public Course getCourse(){return course;} public Branch getBranch(){return branch;} public String getCode(){return code;} public LocalDate getEnrollmentStartsOn(){return enrollmentStartsOn;} public LocalDate getEnrollmentEndsOn(){return enrollmentEndsOn;} public Status getStatus(){return status;}
 public void changeStatus(Status value){status=value;}
}
