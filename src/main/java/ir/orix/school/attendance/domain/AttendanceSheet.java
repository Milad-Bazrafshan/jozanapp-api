package ir.orix.school.attendance.domain;

import java.time.Instant; import jakarta.persistence.*;
import ir.orix.school.identity.domain.UserAccount; import ir.orix.school.organization.domain.Tenant; import ir.orix.school.scheduling.domain.ClassSession; import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name="attendance_sheet",schema="school")
public class AttendanceSheet extends AuditedEntity {
 public enum Status{DRAFT,FINALIZED}
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="class_session_id") private ClassSession classSession;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="finalized_by") private UserAccount finalizedBy;
 @Column(name="finalized_at") private Instant finalizedAt;
 protected AttendanceSheet(){}
 public AttendanceSheet(Tenant tenant,ClassSession session){this.tenant=tenant;this.classSession=session;this.status=Status.DRAFT;}
 public void finalizeBy(UserAccount actor,Instant now){this.status=Status.FINALIZED;this.finalizedBy=actor;this.finalizedAt=now;}
 public ClassSession getClassSession(){return classSession;} public Status getStatus(){return status;} public UserAccount getFinalizedBy(){return finalizedBy;} public Instant getFinalizedAt(){return finalizedAt;}
}
