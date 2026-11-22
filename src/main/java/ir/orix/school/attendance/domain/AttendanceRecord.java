package ir.orix.school.attendance.domain;

import java.time.Instant; import jakarta.persistence.*;
import ir.orix.school.classroom.domain.ClassSection; import ir.orix.school.enrollment.domain.Enrollment; import ir.orix.school.identity.domain.UserAccount; import ir.orix.school.organization.domain.Tenant; import ir.orix.school.people.domain.StudentProfile; import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name="attendance_record",schema="school")
public class AttendanceRecord extends AuditedEntity {
 public enum PresenceStatus{PRESENT,ABSENT,LATE,EXCUSED}
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="attendance_sheet_id") private AttendanceSheet sheet;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="student_id") private StudentProfile student;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="enrollment_id") private Enrollment enrollment;
 @Enumerated(EnumType.STRING) @Column(name="presence_status",nullable=false,length=30) private PresenceStatus presenceStatus;
 @Enumerated(EnumType.STRING) @Column(name="delivery_mode",nullable=false,length=30) private ClassSection.DeliveryMode deliveryMode;
 @Column(name="late_minutes") private Integer lateMinutes; @Column(length=500) private String note;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="marked_by") private UserAccount markedBy; @Column(name="marked_at",nullable=false) private Instant markedAt;
 protected AttendanceRecord(){}
 public AttendanceRecord(Tenant tenant,AttendanceSheet sheet,Enrollment enrollment,PresenceStatus status,ClassSection.DeliveryMode mode,Integer lateMinutes,String note,UserAccount actor,Instant now){this.tenant=tenant;this.sheet=sheet;this.student=enrollment.getStudent();this.enrollment=enrollment;change(status,mode,lateMinutes,note,actor,now);}
 public PresenceStatus change(PresenceStatus status,ClassSection.DeliveryMode mode,Integer lateMinutes,String note,UserAccount actor,Instant now){PresenceStatus previous=this.presenceStatus;this.presenceStatus=status;this.deliveryMode=mode;this.lateMinutes=status==PresenceStatus.LATE?lateMinutes:null;this.note=normalize(note);this.markedBy=actor;this.markedAt=now;return previous;}
 private static String normalize(String value){return value==null||value.isBlank()?null:value.strip();}
 public AttendanceSheet getSheet(){return sheet;} public StudentProfile getStudent(){return student;} public Enrollment getEnrollment(){return enrollment;} public PresenceStatus getPresenceStatus(){return presenceStatus;} public ClassSection.DeliveryMode getDeliveryMode(){return deliveryMode;} public Integer getLateMinutes(){return lateMinutes;} public String getNote(){return note;} public UserAccount getMarkedBy(){return markedBy;} public Instant getMarkedAt(){return markedAt;}
}
