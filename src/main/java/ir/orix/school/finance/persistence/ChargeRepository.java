package ir.orix.school.finance.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.finance.domain.Charge;

public interface ChargeRepository extends JpaRepository<Charge, Long> {
    @EntityGraph(attributePaths = {"student", "student.person", "classSection"})
    List<Charge> findAllByTenantIdOrderByDueOnAscCreatedAtDesc(Long tenantId);

    @EntityGraph(attributePaths = {"student", "student.person", "classSection"})
    List<Charge> findAllByTenantIdAndStudentIdOrderByDueOnAscCreatedAtDesc(Long tenantId, Long studentId);

    @EntityGraph(attributePaths = {"student", "student.person", "classSection"})
    Optional<Charge> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);
}
