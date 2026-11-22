package ir.orix.school.meeting.api;

import org.springframework.web.bind.annotation.*;
import ir.orix.school.meeting.application.MeetingService;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/meetings")
public class MeetingController {
    private final MeetingService meetings;
    public MeetingController(MeetingService meetings) { this.meetings = meetings; }

    @PostMapping("/sessions/{sessionId}/join")
    public MeetingService.JoinView join(@PathVariable Long sessionId, @RequestParam(defaultValue = "STUDENT") MeetingService.Role role) {
        return meetings.join(CurrentUser.tenantId(), CurrentUser.membershipId(), sessionId, role);
    }
}
