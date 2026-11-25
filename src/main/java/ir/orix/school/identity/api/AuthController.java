package ir.orix.school.identity.api;

import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ir.orix.school.identity.application.AuthService;
import ir.orix.school.shared.security.AuthCookieService;
import ir.orix.school.shared.security.CurrentUser;
import ir.orix.school.shared.security.SecurityProperties;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthCookieService cookieService;
    private final SecurityProperties securityProperties;

    public AuthController(AuthService authService, AuthCookieService cookieService,
                          SecurityProperties securityProperties) {
        this.authService = authService;
        this.cookieService = cookieService;
        this.securityProperties = securityProperties;
    }

    @GetMapping("/csrf")
    CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @PostMapping("/login")
    LoginApiResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.LoginOutcome outcome = authService.login(request.tenantSlug(), request.login(), request.password());
        if (outcome instanceof AuthService.LoginOutcome.Success success) {
            writeCookies(response, success.credentials());
            return new LoginApiResponse(success.credentials().body(), null);
        }
        AuthService.LoginOutcome.PendingSms pending = (AuthService.LoginOutcome.PendingSms) outcome;
        return new LoginApiResponse(null, new AuthService.PendingSmsResponse(pending.challengeId(), pending.expiresInSeconds()));
    }

    @PostMapping("/login/2fa")
    AuthService.AuthSessionResponse login2fa(@Valid @RequestBody Login2faRequest request, HttpServletResponse response) {
        AuthService.IssuedCredentials issued = authService.completeLoginWithSms(request.tenantSlug(), request.login(),
                request.challengeId(), request.code());
        writeCookies(response, issued);
        return issued.body();
    }

    @PostMapping("/refresh")
    AuthService.AuthSessionResponse refresh(@RequestBody(required = false) TokenRequest request,
                                            HttpServletRequest httpRequest, HttpServletResponse response) {
        String raw = resolveRefreshToken(request, httpRequest);
        AuthService.IssuedCredentials issued = authService.refresh(raw);
        writeCookies(response, issued);
        return issued.body();
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@RequestBody(required = false) TokenRequest request,
                                HttpServletRequest httpRequest, HttpServletResponse response) {
        String raw = resolveRefreshToken(request, httpRequest);
        authService.logout(raw);
        if (raw == null || raw.isBlank()) {
            authService.logoutByAccessToken(cookieService.readAccessToken(httpRequest));
        }
        cookieService.clearSessionCookies(response);
        cookieService.clearCsrfCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    PasswordResetRequested forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        Optional<String> developmentToken = authService.forgotPassword(request.email());
        return new PasswordResetRequested("If the account exists, a reset request has been created.", developmentToken.orElse(null));
    }

    @PostMapping("/reset-password")
    ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletResponse response) {
        authService.resetPassword(request.token(), request.newPassword());
        cookieService.clearSessionCookies(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    AuthService.UserView me() { return authService.me(CurrentUser.membershipId(), CurrentUser.tenantId()); }

    private void writeCookies(HttpServletResponse response, AuthService.IssuedCredentials issued) {
        cookieService.writeSessionCookies(response, issued.accessToken(), issued.refreshToken(),
                securityProperties.accessTokenDuration(), securityProperties.refreshTokenDuration());
    }

    private String resolveRefreshToken(TokenRequest request, HttpServletRequest httpRequest) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            return request.refreshToken();
        }
        return cookieService.readRefreshToken(httpRequest);
    }

    public record LoginRequest(@NotBlank String tenantSlug,
                               @NotBlank @JsonAlias("email") String login,
                               @NotBlank String password) {}

    public record Login2faRequest(@NotBlank String tenantSlug,
                                  @NotBlank @JsonAlias("email") String login,
                                  @NotNull Long challengeId,
                                  @NotBlank @Size(min = 4, max = 10) String code) {}

    public record LoginApiResponse(AuthService.AuthSessionResponse session, AuthService.PendingSmsResponse pendingSms) {}
    public record TokenRequest(String refreshToken) {}
    public record ForgotPasswordRequest(@NotBlank @Email String email) {}
    public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 10, max = 128) String newPassword) {}
    public record PasswordResetRequested(String message, String developmentToken) {}
}
