package ir.orix.school.academic.persistence;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.academic.domain.AcademicPeriod;
public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, Long>{List<AcademicPeriod> findAllByTenantIdOrderByStartsOnDesc(Long tenantId);Optional<AcademicPeriod> findByIdAndTenantId(Long id,Long tenantId);boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId,String code);}
