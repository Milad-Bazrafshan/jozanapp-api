package ir.orix.school.organization.application;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.identity.persistence.UserAccountRepository;
import ir.orix.school.organization.domain.AccessGrant;
import ir.orix.school.organization.domain.AccessRole;
import ir.orix.school.organization.domain.Branch;
import ir.orix.school.organization.domain.EducationalInstitution;
import ir.orix.school.organization.domain.Permission;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.organization.persistence.AccessGrantRepository;
import ir.orix.school.organization.persistence.AccessRoleRepository;
import ir.orix.school.organization.persistence.BranchRepository;
import ir.orix.school.organization.persistence.EducationalInstitutionRepository;
import ir.orix.school.organization.persistence.PermissionRepository;
import ir.orix.school.organization.persistence.TenantMembershipRepository;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.kyc.NationalIdentityResult;
import ir.orix.school.kyc.ZohalThirdService;
import ir.orix.school.people.domain.Person;
import ir.orix.school.people.persistence.PersonRepository;
import ir.orix.school.shared.web.ApiException;

@Service
public class OrganizationService {
    private final TenantRepository tenantRepository;
    private final EducationalInstitutionRepository institutionRepository;
    private final BranchRepository branchRepository;
    private final PermissionRepository permissionRepository;
    private final AccessRoleRepository roleRepository;
    private final UserAccountRepository accountRepository;
    private final PersonRepository personRepository;
    private final TenantMembershipRepository membershipRepository;
    private final AccessGrantRepository grantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final Clock clock;
    private final ZohalThirdService zohal;

    public OrganizationService(TenantRepository tenantRepository, EducationalInstitutionRepository institutionRepository,
            BranchRepository branchRepository, PermissionRepository permissionRepository,
            AccessRoleRepository roleRepository, UserAccountRepository accountRepository,
            PersonRepository personRepository, TenantMembershipRepository membershipRepository,
            AccessGrantRepository grantRepository, PasswordEncoder passwordEncoder, AuditService auditService, Clock clock,
            ZohalThirdService zohal) {
        this.tenantRepository = tenantRepository;
        this.institutionRepository = institutionRepository;
        this.branchRepository = branchRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.accountRepository = accountRepository;
        this.personRepository = personRepository;
        this.membershipRepository = membershipRepository;
        this.grantRepository = grantRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.clock = clock;
        this.zohal = zohal;
    }

    @Transactional(readOnly = true)
    public OrganizationView current(Long tenantId) {
        Tenant tenant = tenant(tenantId);
        EducationalInstitution institution = institutionRepository.findByTenantId(tenantId).orElseThrow(notFound("INSTITUTION_NOT_FOUND", "Institution was not found."));
        return new OrganizationView(tenant.getId(), tenant.getSlug(), tenant.getName(), tenant.getStatus().name(),
                tenant.getTimezone(), institution.getId(), institution.getName(), institution.getType().name(),
                branchRepository.findAllByTenantIdOrderByName(tenantId).size(), tenant.isCourseSelectionEnabled(),
                tenant.isVirtualClassEnabled(), tenant.getSmsProvider(), tenant.getMeetingProvider(), tenant.getMeetingBaseUrl());
    }

    @Transactional(readOnly = true)
    public FeatureView features(Long tenantId) {
        Tenant tenant = tenant(tenantId);
        return new FeatureView(tenant.isCourseSelectionEnabled(), tenant.isVirtualClassEnabled(), tenant.getSmsProvider(),
                tenant.getMeetingProvider(), tenant.getMeetingBaseUrl());
    }

    @Transactional
    public FeatureView updateFeatures(Long tenantId, Long actorId, boolean courseSelection, boolean virtualClass,
            String smsProvider, String meetingProvider, String meetingBaseUrl, String meetingSecret) {
        Tenant tenant = tenant(tenantId);
        tenant.updatePlatformSettings(courseSelection, virtualClass, smsProvider, meetingProvider, meetingBaseUrl, meetingSecret);
        auditService.record(tenantId, actorId, "TENANT_FEATURES_UPDATED", "TENANT", tenantId, Map.of());
        return features(tenantId);
    }

    @Transactional(readOnly = true)
    public List<BranchView> branches(Long tenantId, Long membershipId) {
        List<AccessGrant> grants = grantRepository.findAllByMembershipId(membershipId);
        boolean tenantWide = grants.stream().anyMatch(grant -> grant.getScopeType() == AccessGrant.ScopeType.TENANT
                && grant.getRole().getStatus() == AccessRole.Status.ACTIVE
                && grant.getRole().getPermissions().stream().anyMatch(value -> value.getCode().equals("ORGANIZATION_READ")));
        Set<Long> accessibleBranchIds = grants.stream()
                .filter(grant -> grant.getScopeType() == AccessGrant.ScopeType.BRANCH)
                .filter(grant -> grant.getRole().getStatus() == AccessRole.Status.ACTIVE)
                .filter(grant -> grant.getRole().getPermissions().stream().anyMatch(value -> value.getCode().equals("ORGANIZATION_READ")))
                .map(AccessGrant::getScopeId).collect(java.util.stream.Collectors.toSet());
        return branchRepository.findAllByTenantIdOrderByName(tenantId).stream()
                .filter(branch -> tenantWide || accessibleBranchIds.contains(branch.getId()))
                .map(OrganizationService::branchView).toList();
    }

    @Transactional
    public BranchView createBranch(Long tenantId, Long actorId, String code, String name, String phone, String address) {
        if (branchRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, code)) throw conflict("BRANCH_CODE_EXISTS", "Branch code already exists.");
        Tenant tenant = tenant(tenantId);
        EducationalInstitution institution = institutionRepository.findByTenantId(tenantId).orElseThrow(notFound("INSTITUTION_NOT_FOUND", "Institution was not found."));
        Branch branch = branchRepository.save(new Branch(tenant, institution, code, name, phone, address));
        auditService.record(tenantId, actorId, "BRANCH_CREATED", "BRANCH", branch.getId(), Map.of("code", branch.getCode()));
        return branchView(branch);
    }

    @Transactional
    public BranchView updateBranch(Long tenantId, Long actorId, Long branchId, String name, String phone, String address, Branch.Status status) {
        Branch branch = branchRepository.findByIdAndTenantId(branchId, tenantId).orElseThrow(notFound("BRANCH_NOT_FOUND", "Branch was not found."));
        branch.update(name, phone, address, status);
        auditService.record(tenantId, actorId, "BRANCH_UPDATED", "BRANCH", branch.getId(), Map.of("status", status.name()));
        return branchView(branch);
    }

    @Transactional(readOnly = true)
    public List<PermissionView> permissions() {
        return permissionRepository.findAll().stream().sorted(java.util.Comparator.comparing(Permission::getCode))
                .map(value -> new PermissionView(value.getCode(), value.getModule(), value.getDescription())).toList();
    }

    @Transactional(readOnly = true)
    public List<RoleView> roles(Long tenantId) {
        return roleRepository.findAllByTenantIdOrderByName(tenantId).stream().map(OrganizationService::roleView).toList();
    }

    @Transactional
    public RoleView createRole(Long tenantId, Long actorId, String code, String name, String description, Set<String> permissionCodes) {
        if (roleRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, code)) throw conflict("ROLE_CODE_EXISTS", "Role code already exists.");
        AccessRole role = new AccessRole(tenant(tenantId), code, name, description, false);
        role.replacePermissions(resolvePermissions(permissionCodes));
        roleRepository.save(role);
        auditService.record(tenantId, actorId, "ROLE_CREATED", "ACCESS_ROLE", role.getId(), Map.of("code", role.getCode()));
        return roleView(role);
    }

    @Transactional
    public RoleView updateRole(Long tenantId, Long actorId, Long roleId, String name, String description,
                               Set<String> permissionCodes, AccessRole.Status status) {
        AccessRole role = roleRepository.findByIdAndTenantId(roleId, tenantId).orElseThrow(notFound("ROLE_NOT_FOUND", "Role was not found."));
        role.update(name, description, resolvePermissions(permissionCodes), status);
        auditService.record(tenantId, actorId, "ROLE_UPDATED", "ACCESS_ROLE", role.getId(), Map.of("status", status.name()));
        return roleView(role);
    }

    @Transactional(readOnly = true)
    public List<UserView> users(Long tenantId) {
        return membershipRepository.findAllByTenantIdOrderByPersonLastNameAscPersonFirstNameAsc(tenantId).stream()
                .map(membership -> userView(membership, grantRepository.findAllByMembershipId(membership.getId()))).toList();
    }

    @Transactional
    public UserView createUser(Long tenantId, Long actorId, CreateUser command) {
        Tenant tenant = tenant(tenantId);
        UserAccount account = accountRepository.findByEmailIgnoreCase(command.email()).orElse(null);
        if (account != null && membershipRepository.existsByTenantIdAndUserAccountId(tenantId, account.getId())) {
            throw conflict("MEMBERSHIP_EXISTS", "This user already belongs to the tenant.");
        }
        if (account == null) {
            if (command.initialPassword() == null || command.initialPassword().length() < 10) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INITIAL_PASSWORD_REQUIRED", "A new account requires a password of at least 10 characters.");
            }
            account = accountRepository.save(new UserAccount(command.email(), passwordEncoder.encode(command.initialPassword()), clock.instant()));
        }
        Person person = personRepository.save(new Person(tenant, command.firstName(), command.lastName(), command.nationalId(), command.mobile()));
        TenantMembership membership = membershipRepository.save(new TenantMembership(tenant, account, person, clock.instant()));
        AccessGrant grant = createGrant(tenant, membership, actorId, command.roleId(), command.scopeType(), command.scopeId());
        auditService.record(tenantId, actorId, "USER_CREATED", "TENANT_MEMBERSHIP", membership.getId(), Map.of("email", account.getEmail()));
        return userView(membership, List.of(grant));
    }

    @Transactional
    public UserView createUserFromNationalRegistry(Long tenantId, Long actorId, CreateUserFromRegistry command) {
        NationalIdentityResult identity = zohal.nationalIdentityInquiry(command.nationalCode(), command.birthDate(), command.gender());
        if (personRepository.existsByTenantIdAndNationalId(tenantId, identity.nationalCode())) {
            throw conflict("NATIONAL_ID_EXISTS", "A person with this national ID already exists in the tenant.");
        }
        Tenant tenant = tenant(tenantId);
        UserAccount account = accountRepository.findByEmailIgnoreCase(command.email()).orElse(null);
        if (account != null && membershipRepository.existsByTenantIdAndUserAccountId(tenantId, account.getId())) {
            throw conflict("MEMBERSHIP_EXISTS", "This user already belongs to the tenant.");
        }
        if (account == null) {
            if (command.initialPassword() == null || command.initialPassword().length() < 10) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INITIAL_PASSWORD_REQUIRED", "A new account requires a password of at least 10 characters.");
            }
            account = accountRepository.save(new UserAccount(command.email(), passwordEncoder.encode(command.initialPassword()), clock.instant()));
        }
        Person person = personRepository.save(new Person(tenant, identity.firstName(), identity.lastName(), identity.nationalCode(),
                command.mobile(), null, identity.fatherName(), null));
        TenantMembership membership = membershipRepository.save(new TenantMembership(tenant, account, person, clock.instant()));
        AccessGrant grant = createGrant(tenant, membership, actorId, command.roleId(), command.scopeType(), command.scopeId());
        auditService.record(tenantId, actorId, "USER_CREATED", "TENANT_MEMBERSHIP", membership.getId(),
                Map.of("email", account.getEmail(), "source", "ZOHAL_IDENTITY"));
        return userView(membership, List.of(grant));
    }

    @Transactional
    public UserView changeUserStatus(Long tenantId, Long actorId, Long membershipId, TenantMembership.Status membershipStatus,
                                     UserAccount.Status accountStatus) {
        TenantMembership membership = membershipRepository.findByIdAndTenantId(membershipId, tenantId).orElseThrow(notFound("MEMBERSHIP_NOT_FOUND", "Membership was not found."));
        membership.changeStatus(membershipStatus);
        membership.getUserAccount().changeStatus(accountStatus);
        auditService.record(tenantId, actorId, "USER_STATUS_CHANGED", "TENANT_MEMBERSHIP", membershipId,
                Map.of("membershipStatus", membershipStatus.name(), "accountStatus", accountStatus.name()));
        return userView(membership, grantRepository.findAllByMembershipId(membershipId));
    }

    @Transactional
    public UserView addGrant(Long tenantId, Long actorId, Long membershipId, Long roleId, AccessGrant.ScopeType scopeType, Long scopeId) {
        Tenant tenant = tenant(tenantId);
        TenantMembership membership = membershipRepository.findByIdAndTenantId(membershipId, tenantId).orElseThrow(notFound("MEMBERSHIP_NOT_FOUND", "Membership was not found."));
        createGrant(tenant, membership, actorId, roleId, scopeType, scopeId);
        auditService.record(tenantId, actorId, "ACCESS_GRANTED", "TENANT_MEMBERSHIP", membershipId, Map.of("roleId", roleId.toString()));
        return userView(membership, grantRepository.findAllByMembershipId(membershipId));
    }

    private AccessGrant createGrant(Tenant tenant, TenantMembership membership, Long actorId, Long roleId,
                                    AccessGrant.ScopeType scopeType, Long scopeId) {
        AccessRole role = roleRepository.findByIdAndTenantId(roleId, tenant.getId()).orElseThrow(notFound("ROLE_NOT_FOUND", "Role was not found."));
        if (scopeType == AccessGrant.ScopeType.TENANT && scopeId != null) throw invalidScope();
        if (scopeType == AccessGrant.ScopeType.BRANCH) {
            if (scopeId == null || branchRepository.findByIdAndTenantId(scopeId, tenant.getId()).isEmpty()) throw invalidScope();
        }
        UserAccount actor = accountRepository.findById(actorId).orElseThrow(notFound("ACTOR_NOT_FOUND", "Current account was not found."));
        return grantRepository.save(new AccessGrant(tenant, membership, role, scopeType, scopeId, actor));
    }

    private Set<Permission> resolvePermissions(Set<String> codes) {
        Set<String> requested = codes == null ? Set.of() : codes;
        List<Permission> found = permissionRepository.findAllById(requested);
        if (found.size() != requested.size()) throw new ApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_PERMISSION", "One or more permission codes are unknown.");
        return new LinkedHashSet<>(found);
    }

    private Tenant tenant(Long tenantId) { return tenantRepository.findById(tenantId).orElseThrow(notFound("TENANT_NOT_FOUND", "Tenant was not found.")); }
    private static java.util.function.Supplier<ApiException> notFound(String code, String message) { return () -> new ApiException(HttpStatus.NOT_FOUND, code, message); }
    private static ApiException conflict(String code, String message) { return new ApiException(HttpStatus.CONFLICT, code, message); }
    private static ApiException invalidScope() { return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ACCESS_SCOPE", "Scope does not match the selected tenant or branch."); }

    private static BranchView branchView(Branch branch) { return new BranchView(branch.getId(), branch.getCode(), branch.getName(), branch.getStatus().name(), branch.getPhone(), branch.getAddress()); }
    private static RoleView roleView(AccessRole role) { return new RoleView(role.getId(), role.getCode(), role.getName(), role.getDescription(), role.isSystemRole(), role.getStatus().name(), role.getPermissions().stream().map(Permission::getCode).sorted().toList()); }
    private static UserView userView(TenantMembership membership, List<AccessGrant> grants) {
        return new UserView(membership.getId(), membership.getUserAccount().getId(), membership.getUserAccount().getEmail(),
                membership.getPerson().getFirstName(), membership.getPerson().getLastName(), membership.getPerson().getNationalId(),
                membership.getPerson().getMobile(), membership.getStatus().name(), membership.getUserAccount().getStatus().name(),
                grants.stream().map(grant -> new GrantView(grant.getRole().getId(), grant.getRole().getName(), grant.getScopeType().name(), grant.getScopeId())).toList());
    }

    public record OrganizationView(Long tenantId, String tenantSlug, String tenantName, String tenantStatus, String timezone,
            Long institutionId, String institutionName, String institutionType, int branchCount, boolean courseSelectionEnabled,
            boolean virtualClassEnabled, String smsProvider, String meetingProvider, String meetingBaseUrl) {}
    public record FeatureView(boolean courseSelectionEnabled, boolean virtualClassEnabled, String smsProvider, String meetingProvider,
            String meetingBaseUrl) {}
    public record BranchView(Long id, String code, String name, String status, String phone, String address) {}
    public record PermissionView(String code, String module, String description) {}
    public record RoleView(Long id, String code, String name, String description, boolean systemRole, String status, List<String> permissions) {}
    public record GrantView(Long roleId, String roleName, String scopeType, Long scopeId) {}
    public record UserView(Long membershipId, Long accountId, String email, String firstName, String lastName, String nationalId,
            String mobile, String membershipStatus, String accountStatus, List<GrantView> grants) {}
    public record CreateUser(String email, String initialPassword, String firstName, String lastName, String nationalId,
            String mobile, Long roleId, AccessGrant.ScopeType scopeType, Long scopeId) {}
    public record CreateUserFromRegistry(String email, String initialPassword, @jakarta.validation.constraints.NotBlank String nationalCode,
            @jakarta.validation.constraints.NotBlank String birthDate, Boolean gender, String mobile, Long roleId,
            AccessGrant.ScopeType scopeType, Long scopeId) {}
}
