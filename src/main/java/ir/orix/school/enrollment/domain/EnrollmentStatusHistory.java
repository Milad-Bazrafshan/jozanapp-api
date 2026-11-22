package ir.orix.school.enrollment.domain;

import java.time.Instant; import jakarta.persistence.*; import ir.orix.school.identity.domain.UserAccount; import ir.orix.school.organization.domain.Tenant;

@Entity @Table(name="enrollment_status_history",schema="school")
public class EnrollmentStatusHistory {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="enrollment_id") private Enrollment enrollment;
 @Enumerated(EnumType.STRING) @Column(name="from_status",length=30) private Enrollment.Status fromStatus; @Enumerated(EnumType.STRING) @Column(name="to_status",nullable=false,length=30) private Enrollment.Status toStatus; @Column(length=500) private String reason; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="changed_by") private UserAccount changedBy; @Column(name="changed_at",nullable=false) private Instant changedAt;
 protected EnrollmentStatusHistory(){}
 public EnrollmentStatusHistory(Tenant tenant,Enrollment enrollment,Enrollment.Status from,Enrollment.Status to,String reason,UserAccount actor){this();this.tenant=tenant;this.enrollment=enrollment;this.fromStatus=from;this.toStatus=to;this.reason=reason==null||reason.isBlank()?null:reason.strip();this.changedBy=actor;}
 @PrePersist void create(){changedAt=Instant.now();}
}
