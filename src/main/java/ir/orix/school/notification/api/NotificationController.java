package ir.orix.school.notification.api;

import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ir.orix.school.notification.application.NotificationService;
import ir.orix.school.notification.domain.Announcement;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping("/announcements")
    @PreAuthorize("hasAuthority('NOTIFICATION_READ')")
    public List<NotificationService.AnnouncementView> announcements() {
        return service.list(CurrentUser.tenantId());
    }

    @PostMapping("/announcements")
    @PreAuthorize("hasAuthority('NOTIFICATION_WRITE')")
    public NotificationService.AnnouncementView create(@Valid @RequestBody AnnouncementRequest request) {
        return service.create(CurrentUser.tenantId(), CurrentUser.accountId(), CurrentUser.membershipId(), request.toCommand(), false);
    }

    @PostMapping("/announcements/preview")
    @PreAuthorize("hasAuthority('NOTIFICATION_WRITE')")
    public NotificationService.Preview preview(@Valid @RequestBody AnnouncementRequest request) {
        return service.preview(CurrentUser.tenantId(), CurrentUser.membershipId(), request.toCommand(), false);
    }

    @GetMapping("/announcements/{id}/deliveries")
    @PreAuthorize("hasAuthority('NOTIFICATION_READ')")
    public List<NotificationService.DeliveryView> deliveries(@PathVariable Long id) {
        return service.deliveries(CurrentUser.tenantId(), id);
    }

    @GetMapping("/notifications")
    public NotificationService.InboxView inbox() {
        return service.inbox(CurrentUser.tenantId(), CurrentUser.membershipId());
    }

    @PostMapping("/notifications/{id}/read")
    public NotificationService.InboxItem read(@PathVariable Long id) {
        return service.markRead(CurrentUser.tenantId(), CurrentUser.membershipId(), id);
    }

    @PostMapping("/instructor/announcements")
    public NotificationService.AnnouncementView instructorCreate(@Valid @RequestBody AnnouncementRequest request) {
        return service.create(CurrentUser.tenantId(), CurrentUser.accountId(), CurrentUser.membershipId(), request.toCommand(), true);
    }

    public record AnnouncementRequest(@NotBlank @Size(max = 200) String title, @NotBlank String body,
            @NotNull Announcement.AudienceType audienceType, Long audienceRef, @NotNull Announcement.Channel channel, Instant scheduledAt) {
        NotificationService.Command toCommand() { return new NotificationService.Command(title, body, audienceType, audienceRef, channel, scheduledAt); }
    }
}
