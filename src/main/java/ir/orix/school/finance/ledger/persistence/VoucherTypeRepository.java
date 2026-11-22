package ir.orix.school.finance.ledger.persistence;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.finance.ledger.domain.VoucherType;
public interface VoucherTypeRepository extends JpaRepository<VoucherType, Long> {
    List<VoucherType> findAllByTenantIdOrderByCodeAsc(Long tenantId);
    Optional<VoucherType> findByIdAndTenantId(Long id, Long tenantId);
    Optional<VoucherType> findByTenantIdAndCodeIgnoreCase(Long tenantId, String code);
}
