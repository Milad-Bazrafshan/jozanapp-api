package ir.orix.school.finance.ledger.domain;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "ledger_account", schema = "school")
public class LedgerAccount extends AuditedEntity {
    public enum Type { ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE }
    public enum Status { ACTIVE, INACTIVE }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id") private LedgerAccount parent;
    @Column(nullable = false, length = 40) private String code;
    @Column(nullable = false, length = 200) private String name;
    @Enumerated(EnumType.STRING) @Column(name = "account_type", nullable = false, length = 30) private Type type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    protected LedgerAccount() {}
    public LedgerAccount(Tenant tenant, LedgerAccount parent, String code, String name, Type type) {
        this.tenant = tenant; this.parent = parent; this.code = code.strip(); this.name = name.strip(); this.type = type; this.status = Status.ACTIVE;
    }
    public Tenant getTenant() { return tenant; } public LedgerAccount getParent() { return parent; }
    public String getCode() { return code; } public String getName() { return name; } public Type getType() { return type; }
    public Status getStatus() { return status; }
}
