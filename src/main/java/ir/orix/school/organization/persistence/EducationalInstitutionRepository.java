package ir.orix.school.organization.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.organization.domain.EducationalInstitution;

public interface EducationalInstitutionRepository extends JpaRepository<EducationalInstitution, Long> {
    Optional<EducationalInstitution> findByTenantId(Long tenantId);
}
