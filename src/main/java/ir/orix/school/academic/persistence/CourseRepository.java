package ir.orix.school.academic.persistence;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.academic.domain.Course;
public interface CourseRepository extends JpaRepository<Course, Long>{List<Course> findAllByTenantIdOrderByTitle(Long tenantId);Optional<Course> findByIdAndTenantId(Long id,Long tenantId);boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId,String code);}
