package ir.orix.school.finance.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.finance.domain.InvoiceItem;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
    boolean existsByChargeId(Long chargeId);
    Optional<InvoiceItem> findByChargeId(Long chargeId);
}
