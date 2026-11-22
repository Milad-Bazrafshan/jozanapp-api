package ir.orix.school.workflow.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.workflow.domain.ProcessTask;
public interface ProcessTaskRepository extends JpaRepository<ProcessTask, Long> {
    @EntityGraph(attributePaths = {"instance", "instance.definition", "instance.student", "instance.student.person", "step"})
    List<ProcessTask> findAllByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, ProcessTask.Status status);
    @EntityGraph(attributePaths = {"instance", "step"})
    Optional<ProcessTask> findByIdAndTenantId(Long id, Long tenantId);
    List<ProcessTask> findAllByInstanceIdOrderByCreatedAtAsc(Long instanceId);
}
