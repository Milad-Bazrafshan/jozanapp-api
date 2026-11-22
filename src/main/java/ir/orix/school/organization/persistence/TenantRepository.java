package ir.orix.school.organization.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.organization.domain.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findBySlugIgnoreCase(String slug);
}
