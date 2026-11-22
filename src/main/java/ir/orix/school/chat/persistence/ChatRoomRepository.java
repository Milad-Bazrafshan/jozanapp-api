package ir.orix.school.chat.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ir.orix.school.chat.domain.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @EntityGraph(attributePaths = {"student", "student.person", "classSection"})
    @Query("""
            select r from ChatRoom r where r.tenant.id = :tenantId and (
                r.guardianMembershipId = :membershipId or r.instructorMembershipId = :membershipId
                or r.counselorMembershipId = :membershipId or r.studentMembershipId = :membershipId)
            """)
    List<ChatRoom> findMine(@Param("tenantId") Long tenantId, @Param("membershipId") Long membershipId);

    @EntityGraph(attributePaths = {"student", "student.person", "classSection"})
    Optional<ChatRoom> findByIdAndTenantId(Long id, Long tenantId);

    Optional<ChatRoom> findByTenantIdAndGuardianMembershipIdAndInstructorMembershipIdAndStudentId(
            Long tenantId, Long guardian, Long instructor, Long studentId);

    Optional<ChatRoom> findByTenantIdAndStudentIdAndCounselorMembershipIdAndRoomType(
            Long tenantId, Long studentId, Long counselorMembershipId, ChatRoom.RoomType roomType);
}
