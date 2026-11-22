package ir.orix.school.survey.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.survey.domain.*;
public interface SurveyRepository extends JpaRepository<Survey, Long> {
    @EntityGraph(attributePaths = {"instructor", "instructor.person", "classSection", "questions"})
    List<Survey> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);
    @EntityGraph(attributePaths = {"instructor", "instructor.person", "classSection", "questions"})
    Optional<Survey> findByIdAndTenantId(Long id, Long tenantId);
    @EntityGraph(attributePaths = {"instructor", "instructor.person", "classSection", "questions"})
    List<Survey> findAllByTenantIdAndInstructorIdOrderByCreatedAtDesc(Long tenantId, Long instructorId);
}
