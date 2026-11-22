package ir.orix.school.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.demo-data")
public record DemoDataProperties(boolean enabled, String tenantSlug, String markerStudentCode, String password) {
}
