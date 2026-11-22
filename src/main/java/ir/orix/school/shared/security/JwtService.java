package ir.orix.school.shared.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import ir.orix.school.organization.domain.TenantMembership;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final SecurityProperties properties;
    private final Clock clock;

    public JwtService(JwtEncoder encoder, SecurityProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public String createAccessToken(TenantMembership membership, Collection<String> permissions) {
        return createAccessToken(membership, permissions, Map.of());
    }

    /**
     * Extra claims (e.g. pending2fa, tfChallengeId) are merged into the access token.
     */
    public String createAccessToken(TenantMembership membership, Collection<String> permissions, Map<String, Object> extraClaims) {
        Instant now = clock.instant();
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenDuration()))
                .subject(membership.getUserAccount().getId().toString())
                .id(UUID.randomUUID().toString())
                .claim("tenantId", membership.getTenant().getId().toString())
                .claim("membershipId", membership.getId().toString())
                .claim("email", membership.getUserAccount().getEmail())
                .claim("name", membership.getPerson().getFullName())
                .claim("permissions", permissions);
        extraClaims.forEach(builder::claim);
        return encoder.encode(JwtEncoderParameters.from(builder.build())).getTokenValue();
    }

    public long accessTokenExpiresInSeconds() {
        return properties.accessTokenDuration().toSeconds();
    }
}
