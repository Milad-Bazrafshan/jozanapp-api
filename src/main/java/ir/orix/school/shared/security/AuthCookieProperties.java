package ir.orix.school.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.cookie")
public record AuthCookieProperties(
        boolean secure,
        String sameSite,
        String accessName,
        String refreshName) {
}
