package ir.orix.school.finance.ledger.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.finance.ledger.domain.JournalVoucher;
public interface JournalVoucherRepository extends JpaRepository<JournalVoucher, Long> {
    @EntityGraph(attributePaths = {"voucherType", "lines", "lines.account"})
    List<JournalVoucher> findAllByTenantIdOrderByVoucherDateDescCreatedAtDesc(Long tenantId);
    @EntityGraph(attributePaths = {"voucherType", "lines", "lines.account"})
    Optional<JournalVoucher> findByIdAndTenantId(Long id, Long tenantId);
    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);
    boolean existsByTenantIdAndSourceTypeAndSourceId(Long tenantId, String sourceType, Long sourceId);
}
