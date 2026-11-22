package ir.orix.school.attendance.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.attendance.domain.AttendanceRecord;
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long>{
 @EntityGraph(attributePaths={"student","student.person","enrollment","markedBy"}) List<AttendanceRecord> findAllBySheetIdOrderByStudentPersonLastNameAsc(Long sheetId);
 Optional<AttendanceRecord> findBySheetIdAndStudentId(Long sheetId,Long studentId);
 @EntityGraph(attributePaths={"sheet","sheet.classSession","sheet.classSession.classSection","sheet.classSession.classSection.offering","sheet.classSession.classSection.offering.course","student","student.person"}) List<AttendanceRecord> findAllByTenantIdAndStudentIdOrderByMarkedAtDesc(Long tenantId,Long studentId);
}
