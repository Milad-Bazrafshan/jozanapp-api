package ir.orix.school.assessment.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.assessment.domain.Assessment;
public interface AssessmentRepository extends JpaRepository<Assessment, Long>{ @EntityGraph(attributePaths="course") List<Assessment> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId); @EntityGraph(attributePaths="course") Optional<Assessment> findByIdAndTenantId(Long id,Long tenantId); boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId,String code); }
