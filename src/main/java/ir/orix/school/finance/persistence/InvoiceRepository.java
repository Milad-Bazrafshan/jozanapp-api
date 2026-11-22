package ir.orix.school.finance.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.finance.domain.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    @EntityGraph(attributePaths = {"student", "student.person", "items", "items.charge", "items.charge.student", "items.charge.student.person", "items.charge.classSection"})
    List<Invoice> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);

    @EntityGraph(attributePaths = {"student", "student.person", "items", "items.charge", "items.charge.student", "items.charge.student.person", "items.charge.classSection"})
    List<Invoice> findAllByTenantIdAndStudentIdOrderByCreatedAtDesc(Long tenantId, Long studentId);

    @EntityGraph(attributePaths = {"student", "student.person", "items", "items.charge", "items.charge.student", "items.charge.student.person", "items.charge.classSection"})
    Optional<Invoice> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);
}
