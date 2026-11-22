package ir.orix.school.finance.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.finance.domain.PaymentAllocation;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {
    @EntityGraph(attributePaths = {"payment", "charge"})
    List<PaymentAllocation> findAllByTenantId(Long tenantId);

    @EntityGraph(attributePaths = {"payment", "charge"})
    List<PaymentAllocation> findAllByTenantIdAndChargeStudentId(Long tenantId, Long studentId);

    boolean existsByPaymentIdAndChargeId(Long paymentId, Long chargeId);

    @EntityGraph(attributePaths = {"payment", "charge"})
    List<PaymentAllocation> findAllByPaymentId(Long paymentId);
}
