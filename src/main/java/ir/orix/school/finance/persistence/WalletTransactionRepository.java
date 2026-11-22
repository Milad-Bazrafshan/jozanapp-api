package ir.orix.school.finance.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.finance.domain.WalletTransaction;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    @EntityGraph(attributePaths = {"student", "student.person", "payment"})
    List<WalletTransaction> findAllByTenantIdAndWalletIdOrderByCreatedAtDesc(Long tenantId, Long walletId);
}
