package ir.orix.school.organization.api;

import java.util.List;
import java.util.Set;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.organization.application.OrganizationService;
import ir.orix.school.organization.domain.AccessGrant;
import ir.orix.school.organization.domain.AccessRole;
import ir.orix.school.organization.domain.Branch;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/organization")
public class OrganizationController {
    private final OrganizationService service;
    public OrganizationController(OrganizationService service) { this.service = service; }

    @GetMapping("/current") @PreAuthorize("hasAuthority('ORGANIZATION_READ')")
    OrganizationService.OrganizationView current() { return service.current(CurrentUser.tenantId()); }

    @GetMapping("/features")
    OrganizationService.FeatureView features() { return service.features(CurrentUser.tenantId()); }

    @PatchMapping("/features")
    @PreAuthorize("hasAuthority('FEATURE_WRITE')")
    OrganizationService.FeatureView updateFeatures(@Valid @RequestBody FeatureRequest request) {
        return service.updateFeatures(CurrentUser.tenantId(), CurrentUser.accountId(), request.courseSelectionEnabled(),
                request.virtualClassEnabled(), request.smsProvider(), request.meetingProvider(), request.meetingBaseUrl(), request.meetingSecret());
    }

    @GetMapping("/branches") @PreAuthorize("hasAuthority('ORGANIZATION_READ')")
    List<OrganizationService.BranchView> branches() { return service.branches(CurrentUser.tenantId(), CurrentUser.membershipId()); }

    @PostMapping("/branches") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('BRANCH_WRITE')")
    OrganizationService.BranchView createBranch(@Valid @RequestBody CreateBranchRequest request) {
        return service.createBranch(CurrentUser.tenantId(), CurrentUser.accountId(), request.code(), request.name(), request.phone(), request.address());
    }

    @PatchMapping("/branches/{id}") @PreAuthorize("@scopeAuthorizer.hasPermissionForBranch('BRANCH_WRITE', #id)")
    OrganizationService.BranchView updateBranch(@PathVariable Long id, @Valid @RequestBody UpdateBranchRequest request) {
        return service.updateBranch(CurrentUser.tenantId(), CurrentUser.accountId(), id, request.name(), request.phone(), request.address(), request.status());
    }

    @GetMapping("/permissions") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('ROLE_READ')")
    List<OrganizationService.PermissionView> permissions() { return service.permissions(); }

    @GetMapping("/roles") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('ROLE_READ')")
    List<OrganizationService.RoleView> roles() { return service.roles(CurrentUser.tenantId()); }

    @PostMapping("/roles") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('ROLE_WRITE')")
    OrganizationService.RoleView createRole(@Valid @RequestBody RoleRequest request) {
        return service.createRole(CurrentUser.tenantId(), CurrentUser.accountId(), request.code(), request.name(), request.description(), request.permissions());
    }

    @PatchMapping("/roles/{id}") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('ROLE_WRITE')")
    OrganizationService.RoleView updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return service.updateRole(CurrentUser.tenantId(), CurrentUser.accountId(), id, request.name(), request.description(), request.permissions(), request.status());
    }

    @GetMapping("/users") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('USER_READ')")
    List<OrganizationService.UserView> users() { return service.users(CurrentUser.tenantId()); }

    @PostMapping("/users") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('USER_WRITE')")
    OrganizationService.UserView createUser(@Valid @RequestBody CreateUserRequest request) {
        return service.createUser(CurrentUser.tenantId(), CurrentUser.accountId(), new OrganizationService.CreateUser(
                request.email(), request.initialPassword(), request.firstName(), request.lastName(), request.nationalId(), request.mobile(),
                request.roleId(), request.scopeType(), request.scopeId()));
    }

    @PostMapping("/users/from-national-registry") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('USER_WRITE')")
    OrganizationService.UserView createUserFromRegistry(@Valid @RequestBody CreateUserFromRegistryRequest request) {
        return service.createUserFromNationalRegistry(CurrentUser.tenantId(), CurrentUser.accountId(),
                new OrganizationService.CreateUserFromRegistry(request.email(), request.initialPassword(), request.nationalCode(),
                        request.birthDate(), request.gender(), request.mobile(), request.roleId(), request.scopeType(), request.scopeId()));
    }

    @PatchMapping("/users/{id}/status") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('USER_WRITE')")
    OrganizationService.UserView changeUserStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return service.changeUserStatus(CurrentUser.tenantId(), CurrentUser.accountId(), id, request.membershipStatus(), request.accountStatus());
    }

    @PostMapping("/users/{id}/grants") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('USER_WRITE')")
    OrganizationService.UserView addGrant(@PathVariable Long id, @Valid @RequestBody GrantRequest request) {
        return service.addGrant(CurrentUser.tenantId(), CurrentUser.accountId(), id, request.roleId(), request.scopeType(), request.scopeId());
    }

    public record CreateBranchRequest(@NotBlank @Size(max = 40) String code, @NotBlank @Size(max = 200) String name,
            @Size(max = 30) String phone, @Size(max = 1000) String address) {}
    public record UpdateBranchRequest(@NotBlank @Size(max = 200) String name, @Size(max = 30) String phone,
            @Size(max = 1000) String address, @NotNull Branch.Status status) {}
    public record RoleRequest(@NotBlank @Size(max = 60) String code, @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description, @NotEmpty Set<String> permissions) {}
    public record UpdateRoleRequest(@NotBlank @Size(max = 120) String name, @Size(max = 500) String description,
            @NotEmpty Set<String> permissions, @NotNull AccessRole.Status status) {}
    public record CreateUserRequest(@NotBlank @Email String email, @Size(min = 10, max = 128) String initialPassword,
            @NotBlank @Size(max = 120) String firstName, @NotBlank @Size(max = 120) String lastName,
            @Size(max = 30) String nationalId, @Size(max = 30) String mobile, @NotNull Long roleId,
            @NotNull AccessGrant.ScopeType scopeType, Long scopeId) {}
    public record CreateUserFromRegistryRequest(@NotBlank @Email String email, @Size(min = 10, max = 128) String initialPassword,
            @NotBlank @Size(min = 10, max = 10) String nationalCode, @NotBlank String birthDate, Boolean gender,
            @Size(max = 30) String mobile, @NotNull Long roleId, @NotNull AccessGrant.ScopeType scopeType, Long scopeId) {}
    public record UserStatusRequest(@NotNull TenantMembership.Status membershipStatus, @NotNull UserAccount.Status accountStatus) {}
    public record GrantRequest(@NotNull Long roleId, @NotNull AccessGrant.ScopeType scopeType, Long scopeId) {}
    public record FeatureRequest(boolean courseSelectionEnabled, boolean virtualClassEnabled, String smsProvider,
            String meetingProvider, String meetingBaseUrl, String meetingSecret) {}
}
