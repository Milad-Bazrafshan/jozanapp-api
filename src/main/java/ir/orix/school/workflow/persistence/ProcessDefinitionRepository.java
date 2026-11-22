package ir.orix.school.workflow.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.workflow.domain.*;
public interface ProcessDefinitionRepository extends JpaRepository<ProcessDefinition, Long> {
    @EntityGraph(attributePaths = "steps") List<ProcessDefinition> findAllByTenantIdOrderByNameAsc(Long tenantId);
    @EntityGraph(attributePaths = "steps") Optional<ProcessDefinition> findByIdAndTenantId(Long id, Long tenantId);
    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);
}
