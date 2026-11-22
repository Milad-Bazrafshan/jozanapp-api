package ir.orix.school.workflow.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.workflow.domain.ProcessInstance;
public interface ProcessInstanceRepository extends JpaRepository<ProcessInstance, Long> {
    @EntityGraph(attributePaths = {"definition", "definition.steps", "student", "student.person"})
    List<ProcessInstance> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);
    @EntityGraph(attributePaths = {"definition", "definition.steps", "student", "student.person"})
    List<ProcessInstance> findAllByTenantIdAndStarterMembershipIdOrderByCreatedAtDesc(Long tenantId, Long membershipId);
    @EntityGraph(attributePaths = {"definition", "definition.steps", "student", "student.person"})
    Optional<ProcessInstance> findByIdAndTenantId(Long id, Long tenantId);
}
