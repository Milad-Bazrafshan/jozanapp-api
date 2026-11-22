package ir.orix.school.shared.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record SecurityProperties(
        String issuer,
        String secret,
        Duration accessTokenDuration,
        Duration refreshTokenDuration,
        Duration passwordResetDuration) {
}
