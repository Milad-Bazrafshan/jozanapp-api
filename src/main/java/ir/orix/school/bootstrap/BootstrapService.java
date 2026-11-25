package ir.orix.school.bootstrap;

import java.time.Clock;
import java.time.ZoneId;
import java.util.LinkedHashSet;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.identity.persistence.UserAccountRepository;
import ir.orix.school.organization.domain.AccessGrant;
import ir.orix.school.organization.domain.AccessRole;
import ir.orix.school.organization.domain.Branch;
import ir.orix.school.organization.domain.EducationalInstitution;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.organization.persistence.AccessGrantRepository;
import ir.orix.school.organization.persistence.AccessRoleRepository;
import ir.orix.school.organization.persistence.BranchRepository;
import ir.orix.school.organization.persistence.EducationalInstitutionRepository;
import ir.orix.school.organization.persistence.PermissionRepository;
import ir.orix.school.organization.persistence.TenantMembershipRepository;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.people.domain.Person;
import ir.orix.school.people.persistence.PersonRepository;

@Service
public class BootstrapService {
    private final TenantRepository tenantRepository;
    private final EducationalInstitutionRepository institutionRepository;
    private final BranchRepository branchRepository;
    private final UserAccountRepository accountRepository;
    private final PersonRepository personRepository;
    private final TenantMembershipRepository membershipRepository;
    private final PermissionRepository permissionRepository;
    private final AccessRoleRepository roleRepository;
    private final AccessGrantRepository grantRepository;
    private final PermissionInitializer permissionInitializer;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public BootstrapService(TenantRepository tenantRepository, EducationalInstitutionRepository institutionRepository,
                            BranchRepository branchRepository, UserAccountRepository accountRepository, PersonRepository personRepository,
                            TenantMembershipRepository membershipRepository, PermissionRepository permissionRepository,
                            AccessRoleRepository roleRepository, AccessGrantRepository grantRepository,
                            PermissionInitializer permissionInitializer,
                            PasswordEncoder passwordEncoder, Clock clock) {
        this.tenantRepository = tenantRepository;
        this.institutionRepository = institutionRepository;
        this.branchRepository = branchRepository;
        this.accountRepository = accountRepository;
        this.personRepository = personRepository;
        this.membershipRepository = membershipRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.grantRepository = grantRepository;
        this.permissionInitializer = permissionInitializer;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public void initialize(BootstrapProperties properties) {
        if (!properties.enabled() || tenantRepository.findBySlugIgnoreCase(properties.tenantSlug()).isPresent()) return;
        if (properties.adminPassword() == null || properties.adminPassword().length() < 10) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must contain at least 10 characters");
        }
        Tenant tenant = tenantRepository.save(new Tenant(properties.tenantSlug(), properties.tenantName(), ZoneId.of("Asia/Tehran")));
        permissionInitializer.ensureTenantScopedRoles(tenant);
        EducationalInstitution institution = institutionRepository.save(new EducationalInstitution(
            tenant, properties.tenantName(), EducationalInstitution.Type.GENERAL));
        branchRepository.save(new Branch(tenant, institution, "MAIN", "شعبه اصلی", null, null));
        String adminLogin = properties.adminNationalId() != null && !properties.adminNationalId().isBlank()
                ? properties.adminNationalId().strip()
                : properties.adminEmail() == null ? "" : properties.adminEmail().strip();
        if (adminLogin.isBlank()) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_NATIONAL_ID or BOOTSTRAP_ADMIN_EMAIL is required.");
        }
        UserAccount account = accountRepository.findByEmailIgnoreCase(adminLogin)
            .orElseGet(() -> accountRepository.save(new UserAccount(adminLogin,
                passwordEncoder.encode(properties.adminPassword()), clock.instant())));
        Person person = personRepository.save(new Person(tenant, properties.adminFirstName(), properties.adminLastName(),
                adminLogin, null));
        TenantMembership membership = membershipRepository.save(new TenantMembership(tenant, account, person, clock.instant()));
        AccessRole role = new AccessRole(tenant, "ADMIN", "مدیر سیستم", "دسترسی کامل به مدیریت آموزشگاه", true);
        role.replacePermissions(new LinkedHashSet<>(permissionRepository.findAll()));
        roleRepository.save(role);
        grantRepository.save(new AccessGrant(tenant, membership, role, AccessGrant.ScopeType.TENANT, null, account));
    }
}
