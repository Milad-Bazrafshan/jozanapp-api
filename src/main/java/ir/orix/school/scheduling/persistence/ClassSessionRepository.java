package ir.orix.school.scheduling.persistence;
import java.time.*; import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.scheduling.domain.ClassSession;
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long>{
 @EntityGraph(attributePaths={"classSection","classSection.offering","classSection.offering.course","classSection.offering.branch","scheduleRule","location","instructors","instructors.person","replacementSession"}) List<ClassSession> findAllByTenantIdOrderByPlannedStartAtAsc(Long tenantId);
 @EntityGraph(attributePaths={"classSection","classSection.offering","classSection.offering.course","classSection.offering.branch","scheduleRule","location","instructors","instructors.person","replacementSession"}) Optional<ClassSession> findByIdAndTenantId(Long id,Long tenantId);
 boolean existsByScheduleRuleIdAndSessionDate(Long ruleId,LocalDate date);
 @EntityGraph(attributePaths={"classSection","classSection.offering","classSection.offering.course","classSection.offering.branch","scheduleRule","location","instructors","instructors.person","replacementSession"}) List<ClassSession> findAllByTenantIdAndClassSectionIdInAndPlannedEndAtAfterOrderByPlannedStartAtAsc(Long tenantId,Collection<Long> sectionIds,Instant after);
 @EntityGraph(attributePaths={"classSection","classSection.offering","classSection.offering.course","classSection.offering.branch","scheduleRule","location","instructors","instructors.person","replacementSession"}) List<ClassSession> findAllByTenantIdAndClassSectionIdInOrderByPlannedStartAtAsc(Long tenantId,Collection<Long> sectionIds);
}
