package ir.orix.school.scheduling.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.scheduling.domain.AcademicLocation;
public interface AcademicLocationRepository extends JpaRepository<AcademicLocation, Long>{
 @EntityGraph(attributePaths="branch") List<AcademicLocation> findAllByTenantIdOrderByName(Long tenantId);
 Optional<AcademicLocation> findByIdAndTenantId(Long id,Long tenantId);
 boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId,String code);
}
