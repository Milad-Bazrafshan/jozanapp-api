package ir.orix.school.counseling.domain;

import java.time.LocalDate;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.people.domain.GuardianProfile;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "counselor_assignment", schema = "school")
public class CounselorAssignment extends AuditedEntity {
    public enum AssignmentType { STUDENT, GUARDIAN }
    public enum Status { ACTIVE, ENDED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "counselor_membership_id") private TenantMembership counselorMembership;
    @Enumerated(EnumType.STRING) @Column(name = "assignment_type", nullable = false, length = 30) private AssignmentType assignmentType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id") private StudentProfile student;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "guardian_id") private GuardianProfile guardian;
    @Column(name = "effective_from", nullable = false) private LocalDate effectiveFrom;
    @Column(name = "effective_to") private LocalDate effectiveTo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(length = 500) private String note;

    protected CounselorAssignment() {}

    public CounselorAssignment(Tenant tenant, TenantMembership counselor, AssignmentType type, StudentProfile student,
            GuardianProfile guardian, LocalDate effectiveFrom, String note) {
        this.tenant = tenant;
        this.counselorMembership = counselor;
        this.assignmentType = type;
        this.student = student;
        this.guardian = guardian;
        this.effectiveFrom = effectiveFrom;
        this.status = Status.ACTIVE;
        this.note = note;
    }

    public void end(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
        this.status = Status.ENDED;
    }

    public Tenant getTenant() { return tenant; }
    public TenantMembership getCounselorMembership() { return counselorMembership; }
    public AssignmentType getAssignmentType() { return assignmentType; }
    public StudentProfile getStudent() { return student; }
    public GuardianProfile getGuardian() { return guardian; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public Status getStatus() { return status; }
    public String getNote() { return note; }
}
