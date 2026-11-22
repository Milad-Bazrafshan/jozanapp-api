package ir.orix.school.reportcard.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.reportcard.domain.ReportCard;
public interface ReportCardRepository extends JpaRepository<ReportCard, Long> {
    @EntityGraph(attributePaths = {"student", "student.person", "run", "run.period", "run.classSection", "run.classSection.offering", "run.classSection.offering.course"})
    List<ReportCard> findAllByRunIdOrderByStudentPersonLastNameAsc(Long runId);
    @EntityGraph(attributePaths = {"student", "student.person", "run", "run.period", "run.classSection", "run.classSection.offering", "run.classSection.offering.course"})
    Optional<ReportCard> findByIdAndTenantId(Long id, Long tenantId);
    @EntityGraph(attributePaths = {"student", "student.person", "run", "run.period", "run.classSection", "run.classSection.offering", "run.classSection.offering.course"})
    List<ReportCard> findAllByTenantIdAndStudentIdAndStatusOrderByCreatedAtDesc(Long tenantId, Long studentId, ReportCard.Status status);
    @EntityGraph(attributePaths = {"student", "student.person", "run", "run.period", "run.classSection"})
    Optional<ReportCard> findByVerificationToken(String token);
}
