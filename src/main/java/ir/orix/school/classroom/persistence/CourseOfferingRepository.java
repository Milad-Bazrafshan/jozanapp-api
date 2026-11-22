package ir.orix.school.classroom.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.classroom.domain.CourseOffering;
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long>{
 @EntityGraph(attributePaths={"academicPeriod","course","branch"}) List<CourseOffering> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);
 @EntityGraph(attributePaths={"academicPeriod","course","branch"}) Optional<CourseOffering> findByIdAndTenantId(Long id,Long tenantId);
 boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId,String code);
}
