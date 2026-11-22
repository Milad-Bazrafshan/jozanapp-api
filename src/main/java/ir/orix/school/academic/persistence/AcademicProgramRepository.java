package ir.orix.school.academic.persistence;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.academic.domain.AcademicProgram;
public interface AcademicProgramRepository extends JpaRepository<AcademicProgram, Long>{List<AcademicProgram> findAllByTenantIdOrderByName(Long tenantId);Optional<AcademicProgram> findByIdAndTenantId(Long id,Long tenantId);boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId,String code);}
