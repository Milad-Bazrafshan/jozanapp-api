package ir.orix.school.assessment.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.assessment.domain.AssessmentTarget;
public interface AssessmentTargetRepository extends JpaRepository<AssessmentTarget, Long>{
 @EntityGraph(attributePaths={"classSection","classSection.offering","classSection.offering.branch","classSection.offering.course"}) List<AssessmentTarget> findAllByAssessmentId(Long assessmentId);
 boolean existsByAssessmentIdAndClassSectionId(Long assessmentId,Long sectionId);
 @EntityGraph(attributePaths={"assessment","assessment.course","classSection","classSection.offering","classSection.offering.branch"}) List<AssessmentTarget> findAllByClassSectionIdIn(Collection<Long> sectionIds);
}
