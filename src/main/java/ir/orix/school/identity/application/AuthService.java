package ir.orix.school.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.identity.domain.PasswordResetToken;
import ir.orix.school.identity.domain.RefreshToken;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.identity.persistence.PasswordResetTokenRepository;
import ir.orix.school.identity.domain.TwoFactorSmsChallenge;
import ir.orix.school.identity.persistence.RefreshTokenRepository;
import ir.orix.school.identity.persistence.UserAccountRepository;
import ir.orix.school.organization.domain.AccessGrant;
import ir.orix.school.organization.domain.AccessRole;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.organization.persistence.AccessGrantRepository;
import ir.orix.school.organization.persistence.TenantMembershipRepository;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.kyc.ZohalThirdService;
import ir.orix.school.shared.security.JwtService;
import ir.orix.school.shared.security.SecurityProperties;
import ir.orix.school.shared.security.TokenHashService;
import ir.orix.school.shared.web.ApiException;

@Service
public class AuthService {
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final AccessGrantRepository grantRepository;
    private final UserAccountRepository accountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtDecoder jwtDecoder;
    private final TokenHashService tokenHashService;
    private final SecurityProperties properties;
    private final AuditService auditService;
    private final TwoFactorSmsService twoFactorSmsService;
    private final Clock clock;
    private final boolean exposeResetToken;

    public AuthService(TenantRepository tenantRepository, TenantMembershipRepository membershipRepository,
                       AccessGrantRepository grantRepository, UserAccountRepository accountRepository,
                       RefreshTokenRepository refreshTokenRepository, PasswordResetTokenRepository resetTokenRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService, JwtDecoder jwtDecoder,
                       TokenHashService tokenHashService, SecurityProperties properties, AuditService auditService,
                       TwoFactorSmsService twoFactorSmsService, Clock clock,
                       @Value("${app.security.expose-password-reset-token:false}") boolean exposeResetToken) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.grantRepository = grantRepository;
        this.accountRepository = accountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtDecoder = jwtDecoder;
        this.tokenHashService = tokenHashService;
        this.properties = properties;
        this.auditService = auditService;
        this.twoFactorSmsService = twoFactorSmsService;
        this.clock = clock;
        this.exposeResetToken = exposeResetToken;
    }

    @Transactional
    public LoginOutcome login(String tenantSlug, String login, String password) {
        Tenant tenant = tenantRepository.findBySlugIgnoreCase(tenantSlug.strip())
                .filter(value -> value.getStatus() == Tenant.Status.ACTIVE)
                .orElseThrow(AuthService::invalidCredentials);
        TenantMembership membership = resolveActiveMembership(tenant.getId(), login);
        UserAccount account = membership.getUserAccount();
        if (account.getStatus() != UserAccount.Status.ACTIVE || !passwordEncoder.matches(password, account.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (account.isTwoFactorSmsEnabled()) {
            TwoFactorSmsService.SendCodeResult sent = twoFactorSmsService.send(account, membership.getPerson(), tenant,
                    TwoFactorSmsChallenge.Purpose.LOGIN, tenant.getId());
            auditService.record(tenant.getId(), account.getId(), "AUTH_LOGIN_SMS_SENT", "USER_ACCOUNT", account.getId(), Map.of());
            return new LoginOutcome.PendingSms(sent.challengeId(), sent.expiresInSeconds());
        }
        account.recordLogin(clock.instant());
        accountRepository.save(account);
        auditService.record(tenant.getId(), account.getId(), "AUTH_LOGIN", "USER_ACCOUNT", account.getId(), Map.of());
        return new LoginOutcome.Success(toCredentials(issueSession(membership)));
    }

    @Transactional
    public IssuedCredentials completeLoginWithSms(String tenantSlug, String login, Long challengeId, String code) {
        Tenant tenant = tenantRepository.findBySlugIgnoreCase(tenantSlug.strip())
                .filter(value -> value.getStatus() == Tenant.Status.ACTIVE)
                .orElseThrow(AuthService::invalidCredentials);
        TenantMembership membership = resolveActiveMembership(tenant.getId(), login);
        UserAccount account = membership.getUserAccount();
        if (!account.isTwoFactorSmsEnabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "2FA_NOT_ENABLED", "Two-factor SMS is not enabled for this account.");
        }
        twoFactorSmsService.verifyAndConsume(tenant.getId(), account.getId(), challengeId, code, TwoFactorSmsChallenge.Purpose.LOGIN);
        account.recordLogin(clock.instant());
        accountRepository.save(account);
        auditService.record(tenant.getId(), account.getId(), "AUTH_LOGIN", "USER_ACCOUNT", account.getId(), Map.of());
        return toCredentials(issueSession(membership));
    }

    @Transactional
    public IssuedCredentials refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is missing.");
        }
        Instant now = clock.instant();
        RefreshToken current = refreshTokenRepository.findByTokenHash(tokenHashService.hash(rawToken))
                .filter(value -> value.isUsableAt(now))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired."));
        TenantMembership membership = current.getMembership();
        if (membership.getStatus() != TenantMembership.Status.ACTIVE
                || membership.getUserAccount().getStatus() != UserAccount.Status.ACTIVE
                || membership.getTenant().getStatus() != Tenant.Status.ACTIVE) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INACTIVE_ACCOUNT", "This account is not active.");
        }
        IssuedSession issued = issueSession(membership);
        current.revoke(now, issued.refreshTokenId());
        refreshTokenRepository.save(current);
        return toCredentials(issued);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(tokenHashService.hash(rawToken)).ifPresent(token -> {
            if (token.isUsableAt(clock.instant())) {
                token.revoke(clock.instant(), null);
                refreshTokenRepository.save(token);
                auditService.record(token.getTenant().getId(), token.getUserAccount().getId(), "AUTH_LOGOUT", "USER_ACCOUNT",
                        token.getUserAccount().getId(), Map.of());
            }
        });
    }

    @Transactional
    public void logoutByAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        try {
            Jwt jwt = jwtDecoder.decode(accessToken);
            Long accountId = Long.parseLong(jwt.getSubject());
            Instant now = clock.instant();
            refreshTokenRepository.revokeAllByAccountId(accountId, now);
            auditService.record(null, accountId, "AUTH_LOGOUT", "USER_ACCOUNT", accountId, Map.of());
        } catch (JwtException | IllegalArgumentException ignored) {
            // Cookies are still cleared even when the access token cannot be parsed.
        }
    }

    @Transactional
    public Optional<String> forgotPassword(String email) {
        Optional<UserAccount> account = accountRepository.findByEmailIgnoreCase(email.strip());
        if (account.isEmpty() || account.get().getStatus() != UserAccount.Status.ACTIVE) return Optional.empty();
        String rawToken = tokenHashService.newToken();
        resetTokenRepository.save(new PasswordResetToken(tokenHashService.hash(rawToken), account.get(),
                clock.instant().plus(properties.passwordResetDuration())));
        return exposeResetToken ? Optional.of(rawToken) : Optional.empty();
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        Instant now = clock.instant();
        PasswordResetToken token = resetTokenRepository.findByTokenHash(tokenHashService.hash(rawToken))
                .filter(value -> value.isUsableAt(now))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "Reset token is invalid or expired."));
        UserAccount account = token.getUserAccount();
        account.changePassword(passwordEncoder.encode(newPassword), now);
        token.markUsed(now);
        accountRepository.save(account);
        resetTokenRepository.save(token);
        refreshTokenRepository.revokeAllByAccountId(account.getId(), now);
        auditService.record(null, account.getId(), "PASSWORD_RESET", "USER_ACCOUNT", account.getId(), Map.of());
    }

    @Transactional(readOnly = true)
    public UserView me(Long membershipId, Long tenantId) {
        TenantMembership membership = membershipRepository.findByIdAndTenantId(membershipId, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBERSHIP_NOT_FOUND", "Membership was not found."));
        return userView(membership, grants(membership.getId()));
    }

    private IssuedSession issueSession(TenantMembership membership) {
        List<AccessGrant> grants = grants(membership.getId());
        List<String> permissions = grants.stream()
                .filter(grant -> grant.getRole().getStatus() == AccessRole.Status.ACTIVE)
                .flatMap(grant -> grant.getRole().getPermissions().stream())
                .map(permission -> permission.getCode()).distinct().sorted().toList();
        String accessToken = jwtService.createAccessToken(membership, permissions);
        String rawRefresh = tokenHashService.newToken();
        RefreshToken refreshToken = new RefreshToken(tokenHashService.hash(rawRefresh), membership.getUserAccount(),
                membership, membership.getTenant(), clock.instant().plus(properties.refreshTokenDuration()));
        refreshTokenRepository.save(refreshToken);
        Session session = new Session(accessToken, rawRefresh, jwtService.accessTokenExpiresInSeconds(),
                userView(membership, grants), permissions);
        return new IssuedSession(session, refreshToken.getId());
    }

    private List<AccessGrant> grants(Long membershipId) {
        return grantRepository.findAllByMembershipId(membershipId);
    }

    private UserView userView(TenantMembership membership, List<AccessGrant> grants) {
        List<GrantView> grantViews = grants.stream().map(grant -> new GrantView(
                grant.getRole().getId(), grant.getRole().getCode(), grant.getRole().getName(),
                grant.getScopeType().name(), grant.getScopeId())).toList();
        return new UserView(membership.getUserAccount().getId(), membership.getId(), membership.getTenant().getId(),
                membership.getTenant().getSlug(), membership.getTenant().getName(), membership.getUserAccount().getEmail(),
                membership.getPerson().getFullName(), membership.getUserAccount().isTwoFactorSmsEnabled(), grantViews);
    }

    private IssuedCredentials toCredentials(IssuedSession issued) {
        Session session = issued.session();
        AuthSessionResponse body = new AuthSessionResponse(session.expiresInSeconds(), session.user(), session.permissions());
        return new IssuedCredentials(session.accessToken(), session.refreshToken(), body);
    }

    private TenantMembership resolveActiveMembership(Long tenantId, String login) {
        String trimmed = login == null ? "" : login.strip();
        if (trimmed.isBlank()) {
            throw invalidCredentials();
        }
        Optional<TenantMembership> byNational = Optional.empty();
        try {
            String nationalId = ZohalThirdService.normalizeNationalCode(trimmed);
            byNational = membershipRepository.findByTenantIdAndPersonNationalId(tenantId, nationalId);
        } catch (ApiException ignored) {
            // not a national code; fall back to login identifier on account
        }
        TenantMembership membership = byNational
                .or(() -> membershipRepository.findByTenantIdAndUserAccountEmailIgnoreCase(tenantId, trimmed))
                .filter(value -> value.getStatus() == TenantMembership.Status.ACTIVE)
                .orElseThrow(AuthService::invalidCredentials);
        return membership;
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Tenant, national ID, or password is incorrect.");
    }

    public record GrantView(Long roleId, String roleCode, String roleName, String scopeType, Long scopeId) {}
    public record UserView(Long accountId, Long membershipId, Long tenantId, String tenantSlug, String tenantName,
                           String email, String fullName, boolean twoFactorSmsEnabled, List<GrantView> grants) {}
    private record IssuedSession(Session session, Long refreshTokenId) {}
    public record AuthSessionResponse(long expiresInSeconds, UserView user, List<String> permissions) {}
    public record PendingSmsResponse(Long challengeId, long expiresInSeconds) {}
    public record IssuedCredentials(String accessToken, String refreshToken, AuthSessionResponse body) {}
    public record Session(String accessToken, String refreshToken, long expiresInSeconds,
                          UserView user, List<String> permissions) {}

    public sealed interface LoginOutcome {
        record Success(IssuedCredentials credentials) implements LoginOutcome {}
        record PendingSms(Long challengeId, long expiresInSeconds) implements LoginOutcome {}
    }
}
