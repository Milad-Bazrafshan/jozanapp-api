package ir.orix.school.finance.ledger.domain;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "voucher_type", schema = "school")
public class VoucherType extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @Column(nullable = false, length = 40) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "auto_post", nullable = false) private boolean autoPost;
    protected VoucherType() {}
    public VoucherType(Tenant tenant, String code, String name, boolean autoPost) {
        this.tenant = tenant; this.code = code.strip().toUpperCase(); this.name = name.strip(); this.autoPost = autoPost;
    }
    public Tenant getTenant() { return tenant; } public String getCode() { return code; } public String getName() { return name; }
    public boolean isAutoPost() { return autoPost; }
}
