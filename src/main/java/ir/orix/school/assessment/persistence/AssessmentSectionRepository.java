package ir.orix.school.assessment.persistence;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.assessment.domain.AssessmentSection;
public interface AssessmentSectionRepository extends JpaRepository<AssessmentSection, Long>{ List<AssessmentSection> findAllByAssessmentIdOrderBySequenceNumber(Long assessmentId); Optional<AssessmentSection> findByIdAndTenantId(Long id,Long tenantId); boolean existsByAssessmentIdAndSequenceNumber(Long assessmentId,int sequence); }
