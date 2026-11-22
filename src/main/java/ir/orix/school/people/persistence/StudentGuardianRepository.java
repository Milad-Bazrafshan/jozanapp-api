package ir.orix.school.people.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.people.domain.StudentGuardian;
public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, Long>{
 @EntityGraph(attributePaths={"student.person","guardian.person"}) List<StudentGuardian> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);
 @EntityGraph(attributePaths={"student.person","guardian.person","student","guardian"}) List<StudentGuardian> findAllByTenantIdAndStudentId(Long tenantId,Long studentId);
 @EntityGraph(attributePaths={"student.person","guardian.person","student","guardian"}) List<StudentGuardian> findAllByTenantIdAndGuardianId(Long tenantId,Long guardianId);
}
