package ir.orix.school.counseling.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.counseling.domain.CounselorSeniorReport;

public interface CounselorSeniorReportRepository extends JpaRepository<CounselorSeniorReport, Long> {
    @EntityGraph(attributePaths = {"counselorMembership", "counselorMembership.person", "seniorMembership", "seniorMembership.person"})
    List<CounselorSeniorReport> findAllByTenantIdAndSeniorMembershipIdOrderByReportYearDescReportMonthDesc(Long tenantId, Long seniorMembershipId);

    Optional<CounselorSeniorReport> findByIdAndTenantId(Long id, Long tenantId);

    Optional<CounselorSeniorReport> findByTenantIdAndCounselorMembershipIdAndReportYearAndReportMonth(
            Long tenantId, Long counselorMembershipId, int reportYear, int reportMonth);
}
