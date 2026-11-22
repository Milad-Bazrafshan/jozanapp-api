package ir.orix.school.system.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.stereotype.Service;

import ir.orix.school.system.api.SystemHealthResponse;

@Service
public class SystemHealthService {

    private final HealthEndpoint healthEndpoint;
    private final Clock clock;
    private final String serviceName;
    private final String version;

    public SystemHealthService(
            HealthEndpoint healthEndpoint,
            Clock clock,
            @Value("${spring.application.name}") String serviceName,
            @Value("${app.version}") String version) {
        this.healthEndpoint = healthEndpoint;
        this.clock = clock;
        this.serviceName = serviceName;
        this.version = version;
    }

    public SystemHealthResponse getHealth() {
        return new SystemHealthResponse(
                healthEndpoint.health().getStatus().getCode(),
                serviceName,
                version,
                Instant.now(clock));
    }
}
