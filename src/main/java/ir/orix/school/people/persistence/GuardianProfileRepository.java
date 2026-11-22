package ir.orix.school.people.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.people.domain.GuardianProfile;
public interface GuardianProfileRepository extends JpaRepository<GuardianProfile, Long>{
 @EntityGraph(attributePaths="person") List<GuardianProfile> findAllByTenantIdOrderByPersonLastNameAsc(Long tenantId);
 Optional<GuardianProfile> findByIdAndTenantId(Long id,Long tenantId); boolean existsByTenantIdAndPersonId(Long tenantId,Long personId);
 Optional<GuardianProfile> findByTenantIdAndPersonId(Long tenantId,Long personId);
}
