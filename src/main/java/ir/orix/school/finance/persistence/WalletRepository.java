package ir.orix.school.finance.persistence;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ir.orix.school.finance.domain.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByTenantIdAndGuardianId(Long tenantId, Long guardianId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select wallet from Wallet wallet where wallet.tenant.id = :tenantId and wallet.guardian.id = :guardianId")
    Optional<Wallet> findLocked(@Param("tenantId") Long tenantId, @Param("guardianId") Long guardianId);

    boolean existsByTenantIdAndGuardianId(Long tenantId, Long guardianId);
}
