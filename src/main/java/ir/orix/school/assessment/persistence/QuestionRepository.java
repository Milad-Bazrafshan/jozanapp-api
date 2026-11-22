package ir.orix.school.assessment.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.assessment.domain.Question;
public interface QuestionRepository extends JpaRepository<Question, Long>{ @EntityGraph(attributePaths="course") List<Question> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId); @EntityGraph(attributePaths="course") Optional<Question> findByIdAndTenantId(Long id,Long tenantId); boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId,String code); }
