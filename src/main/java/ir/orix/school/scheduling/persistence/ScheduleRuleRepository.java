package ir.orix.school.scheduling.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.scheduling.domain.ScheduleRule;
public interface ScheduleRuleRepository extends JpaRepository<ScheduleRule, Long>{
 @EntityGraph(attributePaths={"classSection","classSection.offering","classSection.offering.course","location","instructors","instructors.person"}) List<ScheduleRule> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);
 @EntityGraph(attributePaths={"classSection","location","instructors","instructors.person"}) Optional<ScheduleRule> findByIdAndTenantId(Long id,Long tenantId);
 boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId,String code);
}
