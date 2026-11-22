package ir.orix.school.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Reads JWT from HttpOnly access cookie first, then Authorization header (tests/tools only).
 */
@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {
    private final AuthCookieService cookieService;

    public CookieBearerTokenResolver(AuthCookieService cookieService) {
        this.cookieService = cookieService;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        String fromCookie = cookieService.readAccessToken(request);
        if (StringUtils.hasText(fromCookie)) {
            return fromCookie;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).trim();
        }
        return null;
    }
}
