package ir.orix.school.notification.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.notification.domain.Announcement;
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);
    Optional<Announcement> findByIdAndTenantId(Long id, Long tenantId);
}
