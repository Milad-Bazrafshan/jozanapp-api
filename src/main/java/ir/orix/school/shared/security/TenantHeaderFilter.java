package ir.orix.school.shared.security;

import java.io.IOException;
import java.util.Set;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantHeaderFilter extends OncePerRequestFilter {
    public static final String TENANT_HEADER = "X-Tenant-Id";
    private static final Set<String> EXEMPT_PATHS = Set.of(
            "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password");
    private final SecurityProblemWriter problemWriter;

    public TenantHeaderFilter(SecurityProblemWriter problemWriter) { this.problemWriter = problemWriter; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/") || EXEMPT_PATHS.contains(path) || path.startsWith("/api/v1/public/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication) || !authentication.isAuthenticated()) {
            chain.doFilter(request, response);
            return;
        }
        Jwt jwt = jwtAuthentication.getToken();
        String header = request.getHeader(TENANT_HEADER);
        if (header == null || header.isBlank()) {
            problemWriter.write(response, 400, "TENANT_HEADER_REQUIRED", "X-Tenant-Id header is required.");
            return;
        }
        try {
            if (!Long.valueOf(header.strip()).equals(Long.parseLong(jwt.getClaimAsString("tenantId")))) {
                problemWriter.write(response, 403, "TENANT_ACCESS_DENIED", "The token does not belong to this tenant.");
                return;
            }
        } catch (NumberFormatException exception) {
            problemWriter.write(response, 400, "INVALID_TENANT_HEADER", "X-Tenant-Id must be a numeric id.");
            return;
        }
        chain.doFilter(request, response);
    }
}
