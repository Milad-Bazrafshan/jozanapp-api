package ir.orix.school.counseling.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.counseling.domain.StudyPlan;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    @EntityGraph(attributePaths = {"student", "student.person", "items"})
    List<StudyPlan> findAllByTenantIdAndStudentIdOrderByCreatedAtDesc(Long tenantId, Long studentId);

    @EntityGraph(attributePaths = {"student", "student.person", "items"})
    List<StudyPlan> findAllByTenantIdAndCounselorMembershipIdOrderByCreatedAtDesc(Long tenantId, Long counselorMembershipId);

    @EntityGraph(attributePaths = {"student", "student.person", "items"})
    Optional<StudyPlan> findByIdAndTenantId(Long id, Long tenantId);
}
