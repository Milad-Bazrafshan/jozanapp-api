package ir.orix.school.files.domain;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "stored_file", schema = "school")
public class StoredFile extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @Column(name = "original_name", nullable = false, length = 240) private String originalName;
    @Column(name = "content_type", nullable = false, length = 120) private String contentType;
    @Column(name = "size_bytes", nullable = false) private long sizeBytes;
    @Column(name = "storage_key", nullable = false, length = 400) private String storageKey;
    @Column(name = "owner_membership_id") private Long ownerMembershipId;
    @Column(nullable = false, length = 60) private String purpose;

    protected StoredFile() {}
    public StoredFile(Tenant tenant, String originalName, String contentType, long sizeBytes, String storageKey,
            Long ownerMembershipId, String purpose) {
        this.tenant = tenant;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.ownerMembershipId = ownerMembershipId;
        this.purpose = purpose;
    }
    public Tenant getTenant() { return tenant; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStorageKey() { return storageKey; }
    public Long getOwnerMembershipId() { return ownerMembershipId; }
    public String getPurpose() { return purpose; }
}
