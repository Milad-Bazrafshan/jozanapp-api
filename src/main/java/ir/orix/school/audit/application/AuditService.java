package ir.orix.school.audit.application;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import ir.orix.school.audit.domain.AuditLog;
import ir.orix.school.audit.persistence.AuditLogRepository;
import ir.orix.school.shared.config.RequestTraceFilter;

@Service
public class AuditService {
    private final AuditLogRepository repository;
    public AuditService(AuditLogRepository repository) { this.repository = repository; }

    public void record(Long tenantId, Long actorId, String action, String resourceType, Long resourceId,
                       Map<String, Object> metadata) {
        repository.save(new AuditLog(tenantId, actorId, action, resourceType, resourceId,
                MDC.get(RequestTraceFilter.TRACE_KEY), metadata));
    }
}
