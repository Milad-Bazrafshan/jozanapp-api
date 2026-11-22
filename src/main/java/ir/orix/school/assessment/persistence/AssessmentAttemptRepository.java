package ir.orix.school.assessment.persistence;
import java.time.Instant; import java.util.*; import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import ir.orix.school.assessment.domain.AssessmentAttempt;
public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, Long>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @EntityGraph(attributePaths={"participant","participant.assessment","participant.student"}) @Query("select a from AssessmentAttempt a where a.id=:id and a.participant.assessment.tenant.id=:tenantId") Optional<AssessmentAttempt> findLocked(@Param("id") Long id,@Param("tenantId") Long tenantId);
 @EntityGraph(attributePaths={"participant","participant.assessment","participant.student"}) Optional<AssessmentAttempt> findFirstByParticipantIdAndStatusOrderByStartedAtDesc(Long participantId,AssessmentAttempt.Status status);
 long countByParticipantId(Long participantId);
 @EntityGraph(attributePaths={"participant","participant.assessment","participant.student"}) List<AssessmentAttempt> findAllByStatusAndDeadlineAtLessThanEqual(AssessmentAttempt.Status status,Instant deadline);
}
