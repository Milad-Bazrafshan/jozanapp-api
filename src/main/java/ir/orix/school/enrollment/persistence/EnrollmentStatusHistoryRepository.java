package ir.orix.school.enrollment.persistence;
import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.enrollment.domain.EnrollmentStatusHistory;
public interface EnrollmentStatusHistoryRepository extends JpaRepository<EnrollmentStatusHistory, Long>{}
