package ir.orix.school.system.api;

import java.time.Instant;

public record SystemHealthResponse(
        String status,
        String service,
        String version,
        Instant checkedAt) {
}
