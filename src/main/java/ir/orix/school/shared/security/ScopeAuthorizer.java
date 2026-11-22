package ir.orix.school.shared.security;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.organization.domain.AccessGrant;
import ir.orix.school.organization.domain.AccessRole;
import ir.orix.school.organization.persistence.AccessGrantRepository;

@Component("scopeAuthorizer")
public class ScopeAuthorizer {
    private final AccessGrantRepository grantRepository;
    public ScopeAuthorizer(AccessGrantRepository grantRepository) { this.grantRepository = grantRepository; }

    @Transactional(readOnly = true)
    public boolean hasTenantPermission(String permission) {
        return grantRepository.findAllByMembershipId(CurrentUser.membershipId()).stream()
                .anyMatch(grant -> activeWithPermission(grant, permission)
                        && grant.getScopeType() == AccessGrant.ScopeType.TENANT);
    }

    @Transactional(readOnly = true)
    public boolean hasPermissionForBranch(String permission, Long branchId) {
        return grantRepository.findAllByMembershipId(CurrentUser.membershipId()).stream()
                .anyMatch(grant -> activeWithPermission(grant, permission)
                        && (grant.getScopeType() == AccessGrant.ScopeType.TENANT
                            || branchId.equals(grant.getScopeId())));
    }

    @Transactional(readOnly = true)
    public boolean isTenantAdmin() {
        return grantRepository.findAllByMembershipId(CurrentUser.membershipId()).stream()
                .anyMatch(grant -> grant.getScopeType() == AccessGrant.ScopeType.TENANT
                        && grant.getRole().getStatus() == AccessRole.Status.ACTIVE
                        && "ADMIN".equalsIgnoreCase(grant.getRole().getCode()));
    }

    private boolean activeWithPermission(AccessGrant grant, String permission) {
        return grant.getRole().getStatus() == AccessRole.Status.ACTIVE
                && grant.getRole().getPermissions().stream().anyMatch(value -> value.getCode().equals(permission));
    }
}
