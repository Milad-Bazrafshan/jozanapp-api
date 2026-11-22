package ir.orix.school.counseling.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.counseling.domain.StudyPlanItem;

public interface StudyPlanItemRepository extends JpaRepository<StudyPlanItem, Long> {
    Optional<StudyPlanItem> findByIdAndTenantId(Long id, Long tenantId);
    List<StudyPlanItem> findAllByTenantIdAndStudyPlanIdOrderBySortOrderAsc(Long tenantId, Long studyPlanId);
}
