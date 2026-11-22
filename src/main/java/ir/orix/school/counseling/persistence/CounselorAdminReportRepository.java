package ir.orix.school.counseling.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.counseling.domain.CounselorAdminReport;

public interface CounselorAdminReportRepository extends JpaRepository<CounselorAdminReport, Long> {
    @EntityGraph(attributePaths = {"counselorMembership", "counselorMembership.person"})
    List<CounselorAdminReport> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);

    @EntityGraph(attributePaths = {"counselorMembership", "counselorMembership.person"})
    List<CounselorAdminReport> findAllByTenantIdAndCounselorMembershipIdOrderByCreatedAtDesc(Long tenantId, Long membershipId);

    Optional<CounselorAdminReport> findByIdAndTenantId(Long id, Long tenantId);
}
