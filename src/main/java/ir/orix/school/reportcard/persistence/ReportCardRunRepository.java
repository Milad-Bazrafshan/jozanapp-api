package ir.orix.school.reportcard.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.reportcard.domain.*;
public interface ReportCardRunRepository extends JpaRepository<ReportCardRun, Long> {
    @EntityGraph(attributePaths = {"period", "classSection", "classSection.offering", "classSection.offering.course"})
    List<ReportCardRun> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);
    @EntityGraph(attributePaths = {"period", "classSection", "classSection.offering", "classSection.offering.course"})
    Optional<ReportCardRun> findByIdAndTenantId(Long id, Long tenantId);
}
