package ir.orix.school.workflow.domain;

import java.time.Instant;
import jakarta.persistence.*;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "process_task", schema = "school")
public class ProcessTask extends AuditedEntity {
    public enum Status { PENDING, APPROVED, REJECTED, RETURNED }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "instance_id") private ProcessInstance instance;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "step_id") private ProcessStep step;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(length = 500) private String comment;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "acted_by") private UserAccount actedBy;
    @Column(name = "due_at") private Instant dueAt;
    @Column(name = "acted_at") private Instant actedAt;
    protected ProcessTask() {}
    public ProcessTask(Tenant tenant, ProcessInstance instance, ProcessStep step, Instant dueAt) {
        this.tenant = tenant; this.instance = instance; this.step = step; this.dueAt = dueAt; this.status = Status.PENDING;
    }
    public void complete(Status status, String comment, UserAccount actor, Instant now) {
        this.status = status; this.comment = comment; this.actedBy = actor; this.actedAt = now;
    }
    public ProcessInstance getInstance() { return instance; } public ProcessStep getStep() { return step; }
    public Status getStatus() { return status; } public String getComment() { return comment; }
    public Instant getDueAt() { return dueAt; } public Instant getActedAt() { return actedAt; }
}
