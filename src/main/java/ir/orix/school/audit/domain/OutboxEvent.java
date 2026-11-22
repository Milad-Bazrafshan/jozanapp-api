package ir.orix.school.audit.domain;

import java.time.Instant;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "outbox_event", schema = "school")
public class OutboxEvent extends AuditedEntity {
    public enum Status { PENDING, PROCESSED, FAILED }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @Column(name = "event_type", nullable = false, length = 80) private String eventType;
    @Column(name = "aggregate_type", nullable = false, length = 80) private String aggregateType;
    @Column(name = "aggregate_id", nullable = false) private Long aggregateId;
    @Column(nullable = false, columnDefinition = "TEXT") private String payload;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(nullable = false) private int attempts;
    @Column(name = "last_error", length = 500) private String lastError;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "processed_at") private Instant processedAt;

    protected OutboxEvent() {}
    public OutboxEvent(Tenant tenant, String eventType, String aggregateType, Long aggregateId, String payload, Instant now) {
        this.tenant = tenant;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = Status.PENDING;
        this.occurredAt = now;
    }
    public void markProcessed(Instant now) { status = Status.PROCESSED; processedAt = now; }
    public void markFailed(String error) { status = Status.FAILED; attempts++; lastError = error == null ? null : error.substring(0, Math.min(error.length(), 500)); }
    public Tenant getTenant() { return tenant; }
    public String getEventType() { return eventType; }
    public String getAggregateType() { return aggregateType; }
    public Long getAggregateId() { return aggregateId; }
    public String getPayload() { return payload; }
    public Status getStatus() { return status; }
}
