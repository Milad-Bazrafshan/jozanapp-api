package ir.orix.school.assessment.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.assessment.domain.QuestionVersion;
public interface QuestionVersionRepository extends JpaRepository<QuestionVersion, Long>{ @EntityGraph(attributePaths={"question","question.course"}) Optional<QuestionVersion> findFirstByQuestionIdOrderByVersionNumberDesc(Long questionId); @EntityGraph(attributePaths={"question","question.course"}) Optional<QuestionVersion> findByIdAndTenantId(Long id,Long tenantId); }
