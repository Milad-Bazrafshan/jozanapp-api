package ir.orix.school.finance.ledger.persistence;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.finance.ledger.domain.LedgerAccount;
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {
    List<LedgerAccount> findAllByTenantIdOrderByCodeAsc(Long tenantId);
    Optional<LedgerAccount> findByIdAndTenantId(Long id, Long tenantId);
    Optional<LedgerAccount> findByTenantIdAndCodeIgnoreCase(Long tenantId, String code);
    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);
}
