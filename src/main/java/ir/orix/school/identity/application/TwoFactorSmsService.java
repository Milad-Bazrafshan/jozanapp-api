package ir.orix.school.identity.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.identity.domain.TwoFactorSmsChallenge;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.identity.persistence.TwoFactorSmsChallengeRepository;
import ir.orix.school.notification.application.SmsProvider;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.people.domain.Person;
import ir.orix.school.shared.security.TokenHashService;
import ir.orix.school.shared.web.ApiException;

@Service
public class TwoFactorSmsService {
    private static final int SMS_CODE_LENGTH = 6;
    private static final int CHALLENGE_MINUTES = 10;

    private final TwoFactorSmsChallengeRepository challengeRepository;
    private final TokenHashService tokenHashService;
    private final AuditService auditService;
    private final List<SmsProvider> smsProviders;

    public TwoFactorSmsService(TwoFactorSmsChallengeRepository challengeRepository, TokenHashService tokenHashService,
                               AuditService auditService, List<SmsProvider> smsProviders) {
        this.challengeRepository = challengeRepository;
        this.tokenHashService = tokenHashService;
        this.auditService = auditService;
        this.smsProviders = smsProviders;
    }

    @Transactional
    public SendCodeResult send(UserAccount account, Person person, Tenant tenant, TwoFactorSmsChallenge.Purpose purpose,
                              Long tenantId) {
        String phone = person.getMobile();
        if (phone == null || phone.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MOBILE_REQUIRED",
                    "Set a mobile number on your profile before using SMS verification.");
        }
        switch (purpose) {
            case ENABLE_2FA -> {
                if (account.isTwoFactorSmsEnabled()) {
                    throw new ApiException(HttpStatus.CONFLICT, "2FA_ALREADY_ENABLED", "Two-factor authentication is already enabled.");
                }
            }
            case DISABLE_2FA -> {
                if (!account.isTwoFactorSmsEnabled()) {
                    throw new ApiException(HttpStatus.CONFLICT, "2FA_NOT_ENABLED", "Two-factor authentication is not enabled.");
                }
            }
            case LOGIN -> {
            }
        }
        String code = randomDigits(SMS_CODE_LENGTH);
        Instant expires = Instant.now().plus(CHALLENGE_MINUTES, ChronoUnit.MINUTES);
        TwoFactorSmsChallenge challenge = new TwoFactorSmsChallenge(account, purpose, tokenHashService.hash(code), expires);
        challengeRepository.save(challenge);
        SmsProvider provider = resolveProvider(tenant);
        provider.send(phone, "Orix verification code: " + code);
        auditService.record(tenantId, account.getId(), "2FA_SMS_SENT", "USER_ACCOUNT", account.getId(),
                Map.of("purpose", purpose.name()));
        return new SendCodeResult(challenge.getId(), CHALLENGE_MINUTES * 60L);
    }

    @Transactional
    public void verifyAndConsume(Long tenantId, Long accountId, Long challengeId, String rawCode, TwoFactorSmsChallenge.Purpose expectedPurpose) {
        TwoFactorSmsChallenge challenge = challengeRepository.findByIdAndConsumedFalse(challengeId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "CHALLENGE_NOT_FOUND", "Code request was not found or already used."));
        if (!challenge.getAccountId().equals(accountId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CHALLENGE_MISMATCH", "This code does not belong to your account.");
        }
        if (challenge.purposeEnum() != expectedPurpose) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CHALLENGE_PURPOSE_MISMATCH", "Invalid verification step.");
        }
        if (challenge.isExpiredAt(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CHALLENGE_EXPIRED", "The code has expired. Request a new one.");
        }
        if (!tokenHashService.hash(rawCode.strip()).equals(challenge.getCodeHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CODE", "The verification code is incorrect.");
        }
        challenge.consume();
        challengeRepository.save(challenge);
        auditService.record(tenantId, accountId, "2FA_SMS_VERIFIED", "USER_ACCOUNT", accountId, Map.of("purpose", expectedPurpose.name()));
    }

    private SmsProvider resolveProvider(Tenant tenant) {
        String code = tenant.getSmsProvider() == null ? "DEMO" : tenant.getSmsProvider();
        return smsProviders.stream().filter(p -> p.code().equalsIgnoreCase(code)).findFirst()
                .orElseThrow(() -> new IllegalStateException("No SMS provider for code " + code));
    }

    private static String randomDigits(int length) {
        StringBuilder b = new StringBuilder(length);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            b.append(r.nextInt(10));
        }
        return b.toString();
    }

    public record SendCodeResult(Long challengeId, long expiresInSeconds) {
    }
}
