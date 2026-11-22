package ir.orix.school.counseling.application;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.counseling.domain.CounselorAssignment;
import ir.orix.school.counseling.persistence.CounselorAssignmentRepository;
import ir.orix.school.organization.domain.AccessGrant;
import ir.orix.school.organization.domain.AccessRole;
import ir.orix.school.organization.persistence.AccessGrantRepository;
import ir.orix.school.shared.security.CurrentUser;
import ir.orix.school.shared.security.ScopeAuthorizer;
import ir.orix.school.shared.web.ApiException;

@Component
public class CounselingAccess {
    public enum CounselorTier { SENIOR, MID, JUNIOR }

    private final AccessGrantRepository grants;
    private final CounselorAssignmentRepository assignments;
    private final ScopeAuthorizer scopeAuthorizer;

    public CounselingAccess(AccessGrantRepository grants, CounselorAssignmentRepository assignments, ScopeAuthorizer scopeAuthorizer) {
        this.grants = grants;
        this.assignments = assignments;
        this.scopeAuthorizer = scopeAuthorizer;
    }

    @Transactional(readOnly = true)
    public boolean hasCounselingPermission(String permission) {
        return scopeAuthorizer.hasTenantPermission(permission);
    }

    @Transactional(readOnly = true)
    public Optional<CounselorTier> counselorTier(Long membershipId) {
        return grants.findAllByMembershipId(membershipId).stream()
                .filter(grant -> grant.getRole().getStatus() == AccessRole.Status.ACTIVE)
                .map(grant -> tierFromRoleCode(grant.getRole().getCode()))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Transactional(readOnly = true)
    public boolean isTenantAdmin() {
        return scopeAuthorizer.isTenantAdmin();
    }

    @Transactional(readOnly = true)
    public boolean isActiveStudentCounselor(Long tenantId, Long counselorMembershipId, Long studentId) {
        return assignments.findFirstByTenantIdAndStudentIdAndAssignmentTypeAndStatusAndEffectiveToIsNullOrderByEffectiveFromDesc(
                tenantId, studentId, CounselorAssignment.AssignmentType.STUDENT, CounselorAssignment.Status.ACTIVE)
                .map(value -> value.getCounselorMembership().getId().equals(counselorMembershipId))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public void requireStudentCounselorAccess(Long tenantId, Long membershipId, Long studentId) {
        if (isTenantAdmin() || isActiveStudentCounselor(tenantId, membershipId, studentId)) {
            return;
        }
        throw forbidden("COUNSELOR_STUDENT_ACCESS_DENIED");
    }

    @Transactional(readOnly = true)
    public void requireCounselingWrite(Long tenantId, Long membershipId, Long studentId) {
        requireStudentCounselorAccess(tenantId, membershipId, studentId);
        if (!hasCounselingPermission("COUNSELING_WRITE") && !isTenantAdmin()) {
            throw forbidden("COUNSELING_WRITE_REQUIRED");
        }
    }

    @Transactional(readOnly = true)
    public CounselorTier requireCounselorTier(Long membershipId) {
        return counselorTier(membershipId).orElseThrow(() -> forbidden("COUNSELOR_ROLE_REQUIRED"));
    }

    public static ApiException forbidden(String code) {
        return new ApiException(HttpStatus.FORBIDDEN, code, "You are not authorized for this counseling action.");
    }

    private static Optional<CounselorTier> tierFromRoleCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return switch (code.toUpperCase()) {
            case "COUNSELOR_SENIOR" -> Optional.of(CounselorTier.SENIOR);
            case "COUNSELOR_MID" -> Optional.of(CounselorTier.MID);
            case "COUNSELOR_JUNIOR" -> Optional.of(CounselorTier.JUNIOR);
            default -> Optional.empty();
        };
    }

    public Long currentMembershipId() {
        return CurrentUser.membershipId();
    }
}
