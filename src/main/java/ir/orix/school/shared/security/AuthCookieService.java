package ir.orix.school.shared.security;

import java.time.Duration;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(AuthCookieProperties.class)
public class AuthCookieService {
    private final AuthCookieProperties properties;

    public AuthCookieService(AuthCookieProperties properties) {
        this.properties = properties;
    }

    public void writeSessionCookies(HttpServletResponse response, String accessToken, String refreshToken,
                                    Duration accessMaxAge, Duration refreshMaxAge) {
        response.addHeader("Set-Cookie", buildCookie(properties.accessName(), accessToken, accessMaxAge, "/").toString());
        response.addHeader("Set-Cookie", buildCookie(properties.refreshName(), refreshToken, refreshMaxAge,
                "/api/v1/auth").toString());
    }

    public void clearSessionCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildCookie(properties.accessName(), "", Duration.ZERO, "/").toString());
        response.addHeader("Set-Cookie", buildCookie(properties.refreshName(), "", Duration.ZERO, "/api/v1/auth").toString());
        response.addHeader("Set-Cookie", buildCookie(properties.refreshName(), "", Duration.ZERO, "/").toString());
    }

    /** Spring CSRF cookie (readable by the browser). */
    public void clearCsrfCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", ResponseCookie.from("XSRF-TOKEN", "")
                .httpOnly(false)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path("/")
                .maxAge(Duration.ZERO)
                .build()
                .toString());
    }

    public String readRefreshToken(HttpServletRequest request) {
        return readCookie(request, properties.refreshName());
    }

    public String readAccessToken(HttpServletRequest request) {
        return readCookie(request, properties.accessName());
    }

    private String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie buildCookie(String name, String value, Duration maxAge, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
