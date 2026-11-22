package ir.orix.school.audit.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_log", schema = "school")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_id")
    private Long tenantId;
    @Column(name = "actor_user_id")
    private Long actorUserId;
    @Column(nullable = false, length = 100)
    private String action;
    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;
    @Column(name = "resource_id")
    private Long resourceId;
    @Column(name = "trace_id", length = 128)
    private String traceId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new LinkedHashMap<>();
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditLog() {
    }

    public AuditLog(Long tenantId, Long actorUserId, String action, String resourceType, Long resourceId,
            String traceId, Map<String, Object> metadata) {
        this.tenantId = tenantId;
        this.actorUserId = actorUserId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.traceId = traceId;
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
    }

    @PrePersist
    void onCreate() {
        occurredAt = Instant.now();
    }
}
