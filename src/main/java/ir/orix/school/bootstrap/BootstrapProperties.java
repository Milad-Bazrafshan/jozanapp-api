package ir.orix.school.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap")
public record BootstrapProperties(boolean enabled, String tenantName, String tenantSlug, String adminNationalId, String adminEmail,
        String adminPassword, String adminFirstName, String adminLastName) {
}
