package ir.orix.school.people.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.people.domain.InstructorProfile;
public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, Long>{
 @EntityGraph(attributePaths="person") List<InstructorProfile> findAllByTenantIdOrderByPersonLastNameAsc(Long tenantId);
 boolean existsByTenantIdAndInstructorCodeIgnoreCase(Long tenantId,String code); boolean existsByTenantIdAndPersonId(Long tenantId,Long personId);
 Optional<InstructorProfile> findByTenantIdAndPersonId(Long tenantId,Long personId);
 Optional<InstructorProfile> findByIdAndTenantId(Long id,Long tenantId);
}
