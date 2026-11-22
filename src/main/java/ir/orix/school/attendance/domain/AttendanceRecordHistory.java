package ir.orix.school.attendance.domain;

import java.time.Instant; import jakarta.persistence.*;
import ir.orix.school.classroom.domain.ClassSection; import ir.orix.school.identity.domain.UserAccount; import ir.orix.school.organization.domain.Tenant;

@Entity @Table(name="attendance_record_history",schema="school")
public class AttendanceRecordHistory {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="attendance_record_id") private AttendanceRecord record;
 @Enumerated(EnumType.STRING) @Column(name="from_status",length=30) private AttendanceRecord.PresenceStatus fromStatus;
 @Enumerated(EnumType.STRING) @Column(name="to_status",nullable=false,length=30) private AttendanceRecord.PresenceStatus toStatus;
 @Enumerated(EnumType.STRING) @Column(name="delivery_mode",nullable=false,length=30) private ClassSection.DeliveryMode deliveryMode;
 @Column(name="late_minutes") private Integer lateMinutes; @Column(length=500) private String note; @Column(name="change_reason",length=500) private String changeReason;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="changed_by") private UserAccount changedBy; @Column(name="changed_at",nullable=false) private Instant changedAt;
 protected AttendanceRecordHistory(){}
 public AttendanceRecordHistory(Tenant tenant,AttendanceRecord record,AttendanceRecord.PresenceStatus from,AttendanceRecord.PresenceStatus to,ClassSection.DeliveryMode mode,Integer lateMinutes,String note,String reason,UserAccount actor,Instant now){this();this.tenant=tenant;this.record=record;this.fromStatus=from;this.toStatus=to;this.deliveryMode=mode;this.lateMinutes=lateMinutes;this.note=note;this.changeReason=reason;this.changedBy=actor;this.changedAt=now;}
}
