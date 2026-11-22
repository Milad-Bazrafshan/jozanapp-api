package ir.orix.school.notification.domain;

import java.time.Instant;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "sms_delivery", schema = "school")
public class SmsDelivery extends AuditedEntity {
    public enum Status { QUEUED, SENT, FAILED }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "announcement_id") private Announcement announcement;
    @Column(name = "membership_id") private Long membershipId;
    @Column(nullable = false, length = 40) private String phone;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(name = "provider_message_id", length = 120) private String providerMessageId;
    @Column(name = "error_message", length = 500) private String errorMessage;
    @Column(name = "sent_at") private Instant sentAt;
    protected SmsDelivery() {}
    public SmsDelivery(Tenant tenant, Announcement announcement, Long membershipId, String phone) {
        this.tenant = tenant; this.announcement = announcement; this.membershipId = membershipId; this.phone = phone; this.status = Status.QUEUED;
    }
    public void sent(String providerId, Instant now) { status = Status.SENT; providerMessageId = providerId; sentAt = now; }
    public void failed(String error) { status = Status.FAILED; errorMessage = error; }
    public Announcement getAnnouncement() { return announcement; } public String getPhone() { return phone; }
    public Status getStatus() { return status; } public String getProviderMessageId() { return providerMessageId; }
    public String getErrorMessage() { return errorMessage; } public Instant getSentAt() { return sentAt; }
    public Long getMembershipId() { return membershipId; }
}
