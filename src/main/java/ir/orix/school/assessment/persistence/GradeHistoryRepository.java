package ir.orix.school.assessment.persistence;
import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.assessment.domain.GradeHistory;
public interface GradeHistoryRepository extends JpaRepository<GradeHistory, Long>{}
