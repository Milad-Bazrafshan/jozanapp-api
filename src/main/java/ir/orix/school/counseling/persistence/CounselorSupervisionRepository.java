package ir.orix.school.counseling.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.counseling.domain.CounselorSupervision;

public interface CounselorSupervisionRepository extends JpaRepository<CounselorSupervision, Long> {
    List<CounselorSupervision> findAllByTenantIdAndSeniorMembershipIdAndStatus(
            Long tenantId, Long seniorMembershipId, CounselorSupervision.Status status);

    @EntityGraph(attributePaths = {"seniorMembership", "seniorMembership.person", "counselorMembership", "counselorMembership.person"})
    List<CounselorSupervision> findAllByTenantIdAndStatus(Long tenantId, CounselorSupervision.Status status);

    @EntityGraph(attributePaths = {"seniorMembership", "seniorMembership.person", "counselorMembership", "counselorMembership.person"})
    List<CounselorSupervision> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);

    Optional<CounselorSupervision> findByIdAndTenantId(Long id, Long tenantId);
}
