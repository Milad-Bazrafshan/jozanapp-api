package ir.orix.school.survey.persistence;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.survey.domain.SurveyAnswer;
public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, Long> {
    List<SurveyAnswer> findAllByResponseSurveyId(Long surveyId);
}
