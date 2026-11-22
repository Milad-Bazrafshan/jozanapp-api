package ir.orix.school.people.domain;

import java.time.LocalDate;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name="student_guardian",schema="school")
public class StudentGuardian extends AuditedEntity {
    public enum RelationshipType { FATHER, MOTHER, LEGAL_GUARDIAN, OTHER }
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="student_id") private StudentProfile student;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="guardian_id") private GuardianProfile guardian;
    @Enumerated(EnumType.STRING) @Column(name="relationship_type",nullable=false,length=40) private RelationshipType relationshipType;
    @Column(name="is_primary",nullable=false) private boolean primary;
    @Column(name="has_academic_access",nullable=false) private boolean academicAccess;
    @Column(name="has_financial_access",nullable=false) private boolean financialAccess;
    @Column(name="has_pickup_permission",nullable=false) private boolean pickupPermission;
    @Column(name="receives_notifications",nullable=false) private boolean receivesNotifications;
    @Column(name="effective_from",nullable=false) private LocalDate effectiveFrom;
    @Column(name="effective_to") private LocalDate effectiveTo;
    protected StudentGuardian(){}
    public StudentGuardian(Tenant tenant,StudentProfile student,GuardianProfile guardian,RelationshipType type,boolean primary,boolean academic,boolean financial,boolean pickup,boolean notifications,LocalDate from){this.tenant=tenant;this.student=student;this.guardian=guardian;this.relationshipType=type;this.primary=primary;this.academicAccess=academic;this.financialAccess=financial;this.pickupPermission=pickup;this.receivesNotifications=notifications;this.effectiveFrom=from;}
    public StudentProfile getStudent(){return student;} public GuardianProfile getGuardian(){return guardian;} public RelationshipType getRelationshipType(){return relationshipType;} public boolean isPrimary(){return primary;} public boolean hasAcademicAccess(){return academicAccess;} public boolean hasFinancialAccess(){return financialAccess;} public boolean hasPickupPermission(){return pickupPermission;} public boolean isReceivesNotifications(){return receivesNotifications;} public LocalDate getEffectiveFrom(){return effectiveFrom;} public LocalDate getEffectiveTo(){return effectiveTo;}
}
