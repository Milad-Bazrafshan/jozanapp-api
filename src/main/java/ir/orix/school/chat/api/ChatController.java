package ir.orix.school.chat.api;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import ir.orix.school.chat.application.ChatService;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    private final ChatService chat;
    public ChatController(ChatService chat) { this.chat = chat; }

    @GetMapping("/rooms")
    public List<ChatService.RoomView> rooms() { return chat.rooms(CurrentUser.tenantId(), CurrentUser.membershipId()); }

    @GetMapping("/partners")
    public List<ChatService.PartnerView> partners() {
        return chat.partners(CurrentUser.tenantId(), CurrentUser.membershipId());
    }

    @PostMapping("/rooms")
    public ChatService.RoomView open(@Valid @RequestBody OpenRequest request) {
        return chat.open(CurrentUser.tenantId(), CurrentUser.membershipId(), request.studentId(), request.instructorMembershipId(), request.guardianMembershipId());
    }

    @PostMapping("/counselor-rooms")
    public ChatService.RoomView openCounselor(@Valid @RequestBody CounselorOpenRequest request) {
        return chat.openCounselor(CurrentUser.tenantId(), CurrentUser.membershipId(), request.studentId(), request.counselorMembershipId());
    }

    @GetMapping("/rooms/{id}/messages")
    public List<ChatService.MessageView> history(@PathVariable Long id) {
        return chat.history(CurrentUser.tenantId(), CurrentUser.membershipId(), id);
    }

    @PostMapping("/rooms/{id}/messages")
    public ChatService.MessageView send(@PathVariable Long id, @Valid @RequestBody SendRequest request) {
        return chat.send(CurrentUser.tenantId(), CurrentUser.membershipId(), id, request.body(), request.fileId());
    }

    @PostMapping("/rooms/{id}/read")
    public void read(@PathVariable Long id) { chat.markRead(CurrentUser.tenantId(), CurrentUser.membershipId(), id); }

    public record OpenRequest(@NotNull Long studentId, Long instructorMembershipId, Long guardianMembershipId) {}
    public record CounselorOpenRequest(@NotNull Long studentId, Long counselorMembershipId) {}
    public record SendRequest(@NotBlank String body, Long fileId) {}
}
