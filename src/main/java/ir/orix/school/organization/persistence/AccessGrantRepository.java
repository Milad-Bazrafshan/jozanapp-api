package ir.orix.school.organization.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.organization.domain.AccessGrant;

public interface AccessGrantRepository extends JpaRepository<AccessGrant, Long> {
    @EntityGraph(attributePaths = {"role", "role.permissions"})
    List<AccessGrant> findAllByMembershipId(Long membershipId);
}
