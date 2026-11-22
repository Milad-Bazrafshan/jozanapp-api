package ir.orix.school.organization.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.organization.domain.AccessRole;

public interface AccessRoleRepository extends JpaRepository<AccessRole, Long> {
    @EntityGraph(attributePaths = "permissions")
    List<AccessRole> findAllByTenantIdOrderByName(Long tenantId);

    @EntityGraph(attributePaths = "permissions")
    Optional<AccessRole> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);

    @EntityGraph(attributePaths = "permissions")
    List<AccessRole> findAllBySystemRoleTrue();
}
