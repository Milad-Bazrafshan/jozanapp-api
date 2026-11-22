package ir.orix.school.notification.domain;

import java.time.Instant;
import jakarta.persistence.*;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "announcement", schema = "school")
public class Announcement extends AuditedEntity {
    public enum AudienceType { ALL, BRANCH, CLASS, ROLE, MEMBERSHIP }
    public enum Channel { IN_APP, SMS, BOTH }
    public enum Status { DRAFT, QUEUED, SENT }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String body;
    @Enumerated(EnumType.STRING) @Column(name = "audience_type", nullable = false, length = 40) private AudienceType audienceType;
    @Column(name = "audience_ref") private Long audienceRef;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Channel channel;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(name = "scheduled_at") private Instant scheduledAt;
    @Column(name = "sent_at") private Instant sentAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by") private UserAccount createdBy;
    @Column(name = "class_section_id") private Long classSectionId;
    protected Announcement() {}
    public Announcement(Tenant tenant, String title, String body, AudienceType audienceType, Long audienceRef,
            Channel channel, Instant scheduledAt, UserAccount createdBy, Long classSectionId) {
        this.tenant = tenant; this.title = title.strip(); this.body = body.strip(); this.audienceType = audienceType;
        this.audienceRef = audienceRef; this.channel = channel; this.scheduledAt = scheduledAt; this.createdBy = createdBy;
        this.classSectionId = classSectionId; this.status = Status.QUEUED;
    }
    public void markSent(Instant now) { status = Status.SENT; sentAt = now; }
    public Tenant getTenant() { return tenant; } public String getTitle() { return title; } public String getBody() { return body; }
    public AudienceType getAudienceType() { return audienceType; } public Long getAudienceRef() { return audienceRef; }
    public Channel getChannel() { return channel; } public Status getStatus() { return status; }
    public Instant getScheduledAt() { return scheduledAt; } public Instant getSentAt() { return sentAt; }
    public Long getClassSectionId() { return classSectionId; }
}
