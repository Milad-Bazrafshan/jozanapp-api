package ir.orix.school.attendance.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.attendance.domain.AttendanceSheet;
public interface AttendanceSheetRepository extends JpaRepository<AttendanceSheet, Long>{
 @EntityGraph(attributePaths={"classSession","classSession.classSection","classSession.classSection.offering","classSession.classSection.offering.course","classSession.classSection.offering.branch","finalizedBy"}) Optional<AttendanceSheet> findByClassSessionIdAndTenantId(Long sessionId,Long tenantId);
 @EntityGraph(attributePaths={"classSession","classSession.classSection","classSession.classSection.offering","classSession.classSection.offering.course","classSession.classSection.offering.branch"}) List<AttendanceSheet> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
