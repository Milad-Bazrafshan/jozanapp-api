package ir.orix.school.notification.domain;

import java.time.Instant;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "in_app_notification", schema = "school")
public class InAppNotification extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @Column(name = "membership_id", nullable = false) private Long membershipId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "announcement_id") private Announcement announcement;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String body;
    @Column(name = "read_at") private Instant readAt;
    protected InAppNotification() {}
    public InAppNotification(Tenant tenant, Long membershipId, Announcement announcement, String title, String body) {
        this.tenant = tenant; this.membershipId = membershipId; this.announcement = announcement; this.title = title; this.body = body;
    }
    public void markRead(Instant now) { if (readAt == null) readAt = now; }
    public Long getMembershipId() { return membershipId; } public String getTitle() { return title; } public String getBody() { return body; }
    public Instant getReadAt() { return readAt; } public Announcement getAnnouncement() { return announcement; }
}
