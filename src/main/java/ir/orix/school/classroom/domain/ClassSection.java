package ir.orix.school.classroom.domain;

import jakarta.persistence.*; import ir.orix.school.organization.domain.Tenant; import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name="class_section",schema="school")
public class ClassSection extends AuditedEntity {
 public enum DeliveryMode{IN_PERSON,ONLINE,HYBRID} public enum Status{DRAFT,ACTIVE,COMPLETED,CANCELLED}
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="offering_id") private CourseOffering offering;
 @Column(nullable=false,length=60) private String code; @Column(nullable=false,length=200) private String name; @Column(nullable=false) private int capacity;
 @Enumerated(EnumType.STRING) @Column(name="delivery_mode",nullable=false,length=30) private DeliveryMode deliveryMode; @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
 protected ClassSection(){}
 public ClassSection(Tenant tenant,CourseOffering offering,String code,String name,int capacity,DeliveryMode mode){this.tenant=tenant;this.offering=offering;this.code=code.strip().toUpperCase();this.name=name.strip();this.capacity=capacity;this.deliveryMode=mode;this.status=Status.ACTIVE;}
 public Tenant getTenant(){return tenant;} public CourseOffering getOffering(){return offering;} public String getCode(){return code;} public String getName(){return name;} public int getCapacity(){return capacity;} public DeliveryMode getDeliveryMode(){return deliveryMode;} public Status getStatus(){return status;}
 public void update(String name,int capacity,DeliveryMode mode,Status status){this.name=name.strip();this.capacity=capacity;this.deliveryMode=mode;this.status=status;}
}
