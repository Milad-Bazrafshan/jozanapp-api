package ir.orix.school.portal.application;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.identity.application.TwoFactorSmsService;
import ir.orix.school.identity.application.TwoFactorSmsService.SendCodeResult;
import ir.orix.school.identity.domain.TwoFactorSmsChallenge;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.identity.persistence.UserAccountRepository;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.organization.persistence.TenantMembershipRepository;
import ir.orix.school.people.domain.Person;
import ir.orix.school.people.domain.StudentGuardian;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.people.persistence.PersonRepository;
import ir.orix.school.people.persistence.StudentGuardianRepository;
import ir.orix.school.people.persistence.StudentProfileRepository;
import ir.orix.school.shared.web.ApiException;

@Service
public class AccountPortalService {

    private final TenantMembershipRepository membershipRepository;
    private final PersonRepository personRepository;
    private final UserAccountRepository accountRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentGuardianRepository relationships;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final TwoFactorSmsService twoFactorSmsService;

    public AccountPortalService(TenantMembershipRepository membershipRepository,
                                PersonRepository personRepository,
                                UserAccountRepository accountRepository,
                                StudentProfileRepository studentProfileRepository,
                                StudentGuardianRepository relationships,
                                PasswordEncoder passwordEncoder,
                                AuditService auditService,
                                TwoFactorSmsService twoFactorSmsService) {
        this.membershipRepository = membershipRepository;
        this.personRepository = personRepository;
        this.accountRepository = accountRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.relationships = relationships;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.twoFactorSmsService = twoFactorSmsService;
    }

    @Transactional(readOnly = true)
    public AccountSummary accountSummary(Long membershipId, Long tenantId) {
        TenantMembership membership = requireMembership(membershipId, tenantId);
        UserAccount account = membership.getUserAccount();
        Person person = membership.getPerson();
        String fatherFromRelation = resolveFatherGuardianName(tenantId, person.getId());
        return new AccountSummary(
                account.getEmail(),
                person.getFullName(),
                person.getNationalId(),
                person.getMobile(),
                person.getDateOfBirth(),
                person.getFathersName(),
                person.getPlaceOfBirth(),
                fatherFromRelation,
                account.isTwoFactorSmsEnabled());
    }

    private String resolveFatherGuardianName(Long tenantId, Long personId) {
        Optional<StudentProfile> student = studentProfileRepository.findByTenantIdAndPersonId(tenantId, personId);
        if (student.isEmpty()) {
            return null;
        }
        return relationships.findAllByTenantIdAndStudentId(tenantId, student.get().getId()).stream()
                .filter(link -> link.getEffectiveTo() == null && link.getRelationshipType() == StudentGuardian.RelationshipType.FATHER)
                .findFirst()
                .map(link -> link.getGuardian().getPerson().getFullName())
                .orElse(null);
    }

    @Transactional
    public void updateMobile(Long membershipId, Long tenantId, String mobile) {
        TenantMembership membership = requireMembership(membershipId, tenantId);
        Person person = personRepository.findByIdAndTenantId(membership.getPerson().getId(), tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PERSON_NOT_FOUND", "Person was not found."));
        if (mobile == null || mobile.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MOBILE_REQUIRED", "Mobile number is required.");
        }
        person.updateMobile(mobile.strip());
        personRepository.save(person);
        auditService.record(tenantId, membership.getUserAccount().getId(), "PROFILE_MOBILE_UPDATED", "PERSON", person.getId(), Map.of());
    }

    @Transactional
    public void changePassword(Long membershipId, Long tenantId, String currentPassword, String newPassword) {
        TenantMembership membership = requireMembership(membershipId, tenantId);
        UserAccount account = membership.getUserAccount();
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "Current password is incorrect.");
        }
        account.changePassword(passwordEncoder.encode(newPassword), java.time.Instant.now());
        accountRepository.save(account);
        auditService.record(tenantId, account.getId(), "PASSWORD_CHANGED", "USER_ACCOUNT", account.getId(), Map.of());
    }

    @Transactional
    public SendCodeResult requestTwoFactorEnable(Long membershipId, Long tenantId) {
        TenantMembership m = requireMembership(membershipId, tenantId);
        return twoFactorSmsService.send(m.getUserAccount(), m.getPerson(), m.getTenant(),
                TwoFactorSmsChallenge.Purpose.ENABLE_2FA, tenantId);
    }

    @Transactional
    public void confirmTwoFactorEnable(Long membershipId, Long tenantId, Long challengeId, String code) {
        TenantMembership m = requireMembership(membershipId, tenantId);
        UserAccount account = m.getUserAccount();
        twoFactorSmsService.verifyAndConsume(tenantId, account.getId(), challengeId, code, TwoFactorSmsChallenge.Purpose.ENABLE_2FA);
        account.setTwoFactorSmsEnabled(true);
        accountRepository.save(account);
        auditService.record(tenantId, account.getId(), "2FA_SMS_ENABLED", "USER_ACCOUNT", account.getId(), Map.of());
    }

    @Transactional
    public SendCodeResult requestTwoFactorDisable(Long membershipId, Long tenantId) {
        TenantMembership m = requireMembership(membershipId, tenantId);
        return twoFactorSmsService.send(m.getUserAccount(), m.getPerson(), m.getTenant(),
                TwoFactorSmsChallenge.Purpose.DISABLE_2FA, tenantId);
    }

    @Transactional
    public void confirmTwoFactorDisable(Long membershipId, Long tenantId, Long challengeId, String code) {
        TenantMembership m = requireMembership(membershipId, tenantId);
        UserAccount account = m.getUserAccount();
        twoFactorSmsService.verifyAndConsume(tenantId, account.getId(), challengeId, code, TwoFactorSmsChallenge.Purpose.DISABLE_2FA);
        account.setTwoFactorSmsEnabled(false);
        accountRepository.save(account);
        auditService.record(tenantId, account.getId(), "2FA_SMS_DISABLED", "USER_ACCOUNT", account.getId(), Map.of());
    }

    private TenantMembership requireMembership(Long membershipId, Long tenantId) {
        return membershipRepository.findByIdAndTenantId(membershipId, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBERSHIP_NOT_FOUND", "Membership was not found."));
    }

    public record AccountSummary(
            String email,
            String fullName,
            String nationalId,
            String mobile,
            LocalDate dateOfBirth,
            String fathersName,
            String placeOfBirth,
            String fatherNameFromRelation,
            boolean twoFactorSmsEnabled) {
    }
}
