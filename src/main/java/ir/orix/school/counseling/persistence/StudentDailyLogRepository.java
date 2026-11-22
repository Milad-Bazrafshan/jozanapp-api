package ir.orix.school.counseling.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.counseling.domain.StudentDailyLog;

public interface StudentDailyLogRepository extends JpaRepository<StudentDailyLog, Long> {
    @EntityGraph(attributePaths = {"student", "student.person", "entries"})
    Optional<StudentDailyLog> findByTenantIdAndStudentIdAndLogDate(Long tenantId, Long studentId, LocalDate logDate);

    @EntityGraph(attributePaths = {"student", "student.person", "entries"})
    Optional<StudentDailyLog> findByIdAndTenantId(Long id, Long tenantId);

    @EntityGraph(attributePaths = {"student", "student.person", "entries"})
    List<StudentDailyLog> findAllByTenantIdAndStudentIdOrderByLogDateDesc(Long tenantId, Long studentId);

    @EntityGraph(attributePaths = {"student", "student.person", "entries"})
    List<StudentDailyLog> findAllByTenantIdAndStudentIdInAndStatusOrderByLogDateDesc(
            Long tenantId, List<Long> studentIds, StudentDailyLog.Status status);
}
