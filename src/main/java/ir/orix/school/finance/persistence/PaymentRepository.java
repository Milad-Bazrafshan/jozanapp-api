package ir.orix.school.finance.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.finance.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @EntityGraph(attributePaths = {"student", "student.person"})
    List<Payment> findAllByTenantIdOrderByPaidAtDesc(Long tenantId);

    @EntityGraph(attributePaths = {"student", "student.person"})
    List<Payment> findAllByTenantIdAndStudentIdOrderByPaidAtDesc(Long tenantId, Long studentId);

    @EntityGraph(attributePaths = {"student", "student.person"})
    Optional<Payment> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndReceiptNumberIgnoreCase(Long tenantId, String receiptNumber);
    boolean existsByTenantIdAndProviderAndExternalReferenceIgnoreCase(Long tenantId, Payment.Provider provider, String externalReference);
}
