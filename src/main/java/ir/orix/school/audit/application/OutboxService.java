package ir.orix.school.audit.application;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.audit.domain.OutboxEvent;
import ir.orix.school.audit.persistence.OutboxEventRepository;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.persistence.TenantRepository;

@Service
public class OutboxService {
    private final OutboxEventRepository events;
    private final TenantRepository tenants;
    private final Clock clock;

    public OutboxService(OutboxEventRepository events, TenantRepository tenants, Clock clock) {
        this.events = events;
        this.tenants = tenants;
        this.clock = clock;
    }

    @Transactional
    public void enqueue(Long tenantId, String eventType, String aggregateType, Long aggregateId, String payload) {
        Tenant tenant = tenants.findById(tenantId).orElseThrow();
        events.save(new OutboxEvent(tenant, eventType, aggregateType, aggregateId, payload, Instant.now(clock)));
    }
}
