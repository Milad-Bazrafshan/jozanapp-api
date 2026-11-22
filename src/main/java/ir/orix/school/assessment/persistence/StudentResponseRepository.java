package ir.orix.school.assessment.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.assessment.domain.StudentResponse;
public interface StudentResponseRepository extends JpaRepository<StudentResponse, Long>{
 @EntityGraph(attributePaths={"assessmentQuestion","assessmentQuestion.questionVersion","assessmentQuestion.questionVersion.question"}) List<StudentResponse> findAllByAttemptId(Long attemptId);
 Optional<StudentResponse> findByAttemptIdAndAssessmentQuestionId(Long attemptId,Long questionId);
 @EntityGraph(attributePaths={"attempt","attempt.participant","attempt.participant.assessment","assessmentQuestion","assessmentQuestion.questionVersion","assessmentQuestion.questionVersion.question"}) Optional<StudentResponse> findByIdAndTenantId(Long id,Long tenantId);
}
