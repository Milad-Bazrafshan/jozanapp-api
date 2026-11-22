package ir.orix.school.assessment.persistence;
import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.assessment.domain.AssessmentAttemptEvent;
public interface AssessmentAttemptEventRepository extends JpaRepository<AssessmentAttemptEvent, Long>{}
