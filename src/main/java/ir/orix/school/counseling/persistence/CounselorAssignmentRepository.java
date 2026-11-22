package ir.orix.school.counseling.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.counseling.domain.CounselorAssignment;

public interface CounselorAssignmentRepository extends JpaRepository<CounselorAssignment, Long> {
    @EntityGraph(attributePaths = {"counselorMembership", "counselorMembership.person", "student", "student.person", "guardian", "guardian.person"})
    List<CounselorAssignment> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);

    @EntityGraph(attributePaths = {"student", "student.person", "guardian", "guardian.person"})
    List<CounselorAssignment> findAllByTenantIdAndCounselorMembershipIdAndStatusOrderByCreatedAtDesc(
            Long tenantId, Long counselorMembershipId, CounselorAssignment.Status status);

    Optional<CounselorAssignment> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndCounselorMembershipIdAndStudentIdAndAssignmentTypeAndStatusAndEffectiveToIsNull(
            Long tenantId, Long counselorMembershipId, Long studentId, CounselorAssignment.AssignmentType type,
            CounselorAssignment.Status status);

    Optional<CounselorAssignment> findFirstByTenantIdAndStudentIdAndAssignmentTypeAndStatusAndEffectiveToIsNullOrderByEffectiveFromDesc(
            Long tenantId, Long studentId, CounselorAssignment.AssignmentType type, CounselorAssignment.Status status);
}
