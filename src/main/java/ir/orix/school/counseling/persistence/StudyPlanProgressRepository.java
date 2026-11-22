package ir.orix.school.counseling.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.counseling.domain.StudyPlanProgress;

public interface StudyPlanProgressRepository extends JpaRepository<StudyPlanProgress, Long> {
    Optional<StudyPlanProgress> findByTenantIdAndStudyPlanItemId(Long tenantId, Long itemId);
}
