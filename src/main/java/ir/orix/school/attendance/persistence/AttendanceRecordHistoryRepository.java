package ir.orix.school.attendance.persistence;
import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.attendance.domain.AttendanceRecordHistory;
public interface AttendanceRecordHistoryRepository extends JpaRepository<AttendanceRecordHistory, Long>{}
