package ir.orix.school.notification.persistence;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.notification.domain.SmsDelivery;
public interface SmsDeliveryRepository extends JpaRepository<SmsDelivery, Long> {
    List<SmsDelivery> findAllByAnnouncementIdOrderByCreatedAtDesc(Long announcementId);
    List<SmsDelivery> findAllByTenantIdAndStatus(Long tenantId, SmsDelivery.Status status);
}
