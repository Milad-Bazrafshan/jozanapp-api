package ir.orix.school.people.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.people.domain.StudentProfile;
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long>{
 @EntityGraph(attributePaths="person") List<StudentProfile> findAllByTenantIdOrderByPersonLastNameAsc(Long tenantId);
 Optional<StudentProfile> findByIdAndTenantId(Long id,Long tenantId); boolean existsByTenantIdAndStudentCodeIgnoreCase(Long tenantId,String code); boolean existsByTenantIdAndPersonId(Long tenantId,Long personId);
 Optional<StudentProfile> findByTenantIdAndPersonId(Long tenantId,Long personId);
}
