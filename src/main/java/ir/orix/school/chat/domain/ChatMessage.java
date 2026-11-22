package ir.orix.school.chat.domain;

import java.time.Instant;
import jakarta.persistence.*;
import ir.orix.school.files.domain.StoredFile;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "chat_message", schema = "school")
public class ChatMessage extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_id") private ChatRoom room;
    @Column(name = "sender_membership_id", nullable = false) private Long senderMembershipId;
    @Column(nullable = false, columnDefinition = "TEXT") private String body;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "file_id") private StoredFile file;
    @Column(name = "read_at") private Instant readAt;
    protected ChatMessage() {}
    public ChatMessage(Tenant tenant, ChatRoom room, Long senderMembershipId, String body, StoredFile file) {
        this.tenant = tenant; this.room = room; this.senderMembershipId = senderMembershipId; this.body = body; this.file = file;
    }
    public void markRead(Instant now) { if (readAt == null) readAt = now; }
    public ChatRoom getRoom() { return room; } public Long getSenderMembershipId() { return senderMembershipId; }
    public String getBody() { return body; } public StoredFile getFile() { return file; } public Instant getReadAt() { return readAt; }
}
