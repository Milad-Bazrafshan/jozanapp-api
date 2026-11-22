package ir.orix.school.meeting.persistence;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import ir.orix.school.meeting.domain.MeetingRoom;
public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, Long> {
    Optional<MeetingRoom> findByClassSessionId(Long sessionId);
}
