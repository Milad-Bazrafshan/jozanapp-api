package ir.orix.school.workflow.domain;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "process_definition", schema = "school")
public class ProcessDefinition extends AuditedEntity {
    public enum StarterRole { GUARDIAN, STUDENT, INSTRUCTOR, STAFF }
    public enum OutputKind { NONE, CERTIFICATE }
    public enum Status { ACTIVE, INACTIVE }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @Column(nullable = false, length = 60) private String code;
    @Column(nullable = false, length = 200) private String name;
    @Column(length = 500) private String description;
    @Enumerated(EnumType.STRING) @Column(name = "starter_role", nullable = false, length = 40) private StarterRole starterRole;
    @Column(name = "form_schema", columnDefinition = "TEXT") private String formSchema;
    @Enumerated(EnumType.STRING) @Column(name = "output_kind", nullable = false, length = 40) private OutputKind outputKind;
    @Column(name = "output_title", length = 200) private String outputTitle;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("sequenceNumber")
    private List<ProcessStep> steps = new ArrayList<>();
    protected ProcessDefinition() {}
    public ProcessDefinition(Tenant tenant, String code, String name, String description, StarterRole starterRole, String formSchema, OutputKind outputKind, String outputTitle) {
        this.tenant = tenant; this.code = code.strip().toUpperCase(); this.name = name.strip(); this.description = description;
        this.starterRole = starterRole; this.formSchema = formSchema; this.outputKind = outputKind == null ? OutputKind.NONE : outputKind;
        this.outputTitle = outputTitle; this.status = Status.ACTIVE;
    }
    public ProcessStep addStep(int sequence, String name, String approverRole, Integer slaHours) {
        ProcessStep step = new ProcessStep(tenant, this, sequence, name, approverRole, slaHours);
        steps.add(step);
        return step;
    }
    public Tenant getTenant() { return tenant; } public String getCode() { return code; } public String getName() { return name; }
    public String getDescription() { return description; } public StarterRole getStarterRole() { return starterRole; }
    public String getFormSchema() { return formSchema; } public OutputKind getOutputKind() { return outputKind; }
    public String getOutputTitle() { return outputTitle; } public Status getStatus() { return status; } public List<ProcessStep> getSteps() { return steps; }
}
