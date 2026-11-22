package ir.orix.school.workflow.domain;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "process_instance", schema = "school")
public class ProcessInstance extends AuditedEntity {
    public enum Status { OPEN, APPROVED, REJECTED, RETURNED, CANCELLED }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "definition_id") private ProcessDefinition definition;
    @Column(name = "starter_membership_id", nullable = false) private Long starterMembershipId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id") private StudentProfile student;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(name = "form_data", columnDefinition = "TEXT") private String formData;
    @Column(name = "current_step", nullable = false) private int currentStep;
    @Column(name = "certificate_body", columnDefinition = "TEXT") private String certificateBody;
    protected ProcessInstance() {}
    public ProcessInstance(Tenant tenant, ProcessDefinition definition, Long starterMembershipId, StudentProfile student, String formData) {
        this.tenant = tenant; this.definition = definition; this.starterMembershipId = starterMembershipId; this.student = student;
        this.formData = formData; this.status = Status.OPEN; this.currentStep = 1;
    }
    public void advance(int next) { this.currentStep = next; }
    public void close(Status status, String certificateBody) { this.status = status; this.certificateBody = certificateBody; }
    public Tenant getTenant() { return tenant; } public ProcessDefinition getDefinition() { return definition; }
    public Long getStarterMembershipId() { return starterMembershipId; } public StudentProfile getStudent() { return student; }
    public Status getStatus() { return status; } public String getFormData() { return formData; }
    public int getCurrentStep() { return currentStep; } public String getCertificateBody() { return certificateBody; }
}
