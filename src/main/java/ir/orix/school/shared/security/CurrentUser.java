package ir.orix.school.shared.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import ir.orix.school.shared.web.ApiException;
import org.springframework.http.HttpStatus;

public final class CurrentUser {
    private CurrentUser() {}

    public static Jwt jwt() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication) {
            return authentication.getToken();
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication is required.");
    }

    public static Long accountId() { return Long.parseLong(jwt().getSubject()); }
    public static Long tenantId() { return Long.parseLong(jwt().getClaimAsString("tenantId")); }
    public static Long membershipId() { return Long.parseLong(jwt().getClaimAsString("membershipId")); }
}
