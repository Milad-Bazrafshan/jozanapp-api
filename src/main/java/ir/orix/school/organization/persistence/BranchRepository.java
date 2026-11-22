package ir.orix.school.organization.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.organization.domain.Branch;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findAllByTenantIdOrderByName(Long tenantId);
    Optional<Branch> findByIdAndTenantId(Long id, Long tenantId);
    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);
}
