package ir.orix.school.assessment.persistence;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ir.orix.school.assessment.domain.AssessmentResult;
public interface AssessmentResultRepository extends JpaRepository<AssessmentResult, Long>{
 @EntityGraph(attributePaths={"participant","participant.assessment","selectedAttempt"}) Optional<AssessmentResult> findByParticipantId(Long participantId);
 @EntityGraph(attributePaths={"participant","participant.assessment","selectedAttempt"}) List<AssessmentResult> findAllByTenantIdAndParticipantAssessmentIdOrderByParticipantStudentNameSnapshotAsc(Long tenantId,Long assessmentId);
 @EntityGraph(attributePaths={"participant","participant.assessment","selectedAttempt"}) Optional<AssessmentResult> findByIdAndTenantId(Long id,Long tenantId);
 @EntityGraph(attributePaths={"participant","participant.assessment","selectedAttempt"}) List<AssessmentResult> findAllByTenantIdAndParticipantStudentIdAndStatusOrderByCreatedAtDesc(Long tenantId,Long studentId,AssessmentResult.Status status);
 @Query("select r from AssessmentResult r join r.participant p join p.assessment a where a.id = :assessmentId and r.status in :statuses order by r.totalScore desc, r.id asc")
 List<AssessmentResult> findAllForRanking(@Param("assessmentId") Long assessmentId,@Param("statuses") List<AssessmentResult.Status> statuses);
}
