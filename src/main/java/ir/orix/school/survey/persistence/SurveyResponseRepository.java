package ir.orix.school.survey.persistence;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.survey.domain.*;
public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long> {
    boolean existsBySurveyIdAndEnrollmentId(Long surveyId, Long enrollmentId);
    List<SurveyResponse> findAllBySurveyId(Long surveyId);
}
