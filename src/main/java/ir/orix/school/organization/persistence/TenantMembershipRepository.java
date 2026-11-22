package ir.orix.school.organization.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.organization.domain.TenantMembership;

public interface TenantMembershipRepository extends JpaRepository<TenantMembership, Long> {
    @EntityGraph(attributePaths = {"tenant", "userAccount", "person"})
    Optional<TenantMembership> findByTenantIdAndUserAccountEmailIgnoreCase(Long tenantId, String email);

    @EntityGraph(attributePaths = {"tenant", "userAccount", "person"})
    Optional<TenantMembership> findByIdAndTenantId(Long id, Long tenantId);

    @EntityGraph(attributePaths = {"userAccount", "person"})
    List<TenantMembership> findAllByTenantIdOrderByPersonLastNameAscPersonFirstNameAsc(Long tenantId);

    @EntityGraph(attributePaths = {"tenant", "userAccount", "person"})
    Optional<TenantMembership> findByTenantIdAndPersonId(Long tenantId, Long personId);

    boolean existsByTenantIdAndUserAccountId(Long tenantId, Long userAccountId);
}
