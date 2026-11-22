package ir.orix.school.workflow.domain;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "process_step", schema = "school")
public class ProcessStep extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "definition_id") private ProcessDefinition definition;
    @Column(name = "sequence_number", nullable = false) private int sequenceNumber;
    @Column(nullable = false, length = 160) private String name;
    @Column(name = "approver_role", nullable = false, length = 40) private String approverRole;
    @Column(name = "sla_hours") private Integer slaHours;
    @Column(name = "allow_reject", nullable = false) private boolean allowReject = true;
    @Column(name = "allow_return", nullable = false) private boolean allowReturn = true;
    protected ProcessStep() {}
    public ProcessStep(Tenant tenant, ProcessDefinition definition, int sequenceNumber, String name, String approverRole, Integer slaHours) {
        this.tenant = tenant; this.definition = definition; this.sequenceNumber = sequenceNumber; this.name = name.strip();
        this.approverRole = approverRole.strip().toUpperCase(); this.slaHours = slaHours;
    }
    public ProcessDefinition getDefinition() { return definition; } public int getSequenceNumber() { return sequenceNumber; }
    public String getName() { return name; } public String getApproverRole() { return approverRole; } public Integer getSlaHours() { return slaHours; }
}
