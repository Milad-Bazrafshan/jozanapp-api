package ir.orix.school.notification.persistence;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.notification.domain.InAppNotification;
public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {
    List<InAppNotification> findAllByTenantIdAndMembershipIdOrderByCreatedAtDesc(Long tenantId, Long membershipId);
    Optional<InAppNotification> findByIdAndTenantIdAndMembershipId(Long id, Long tenantId, Long membershipId);
    long countByTenantIdAndMembershipIdAndReadAtIsNull(Long tenantId, Long membershipId);
}
