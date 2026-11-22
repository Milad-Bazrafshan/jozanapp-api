package ir.orix.school.audit.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.audit.domain.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
