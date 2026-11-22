package ir.orix.school.reportcard.persistence;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.reportcard.domain.ReportCardLine;
public interface ReportCardLineRepository extends JpaRepository<ReportCardLine, Long> {
    List<ReportCardLine> findAllByReportCardIdOrderByCreatedAtAsc(Long reportCardId);
    void deleteAllByReportCardId(Long reportCardId);
}
