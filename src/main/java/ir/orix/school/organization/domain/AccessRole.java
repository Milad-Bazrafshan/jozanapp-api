package ir.orix.school.organization.domain;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "access_role", schema = "school")
public class AccessRole extends AuditedEntity {

    public enum Status { ACTIVE, INACTIVE }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 60)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "system_role", nullable = false)
    private boolean systemRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permission",
            schema = "school",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_code"))
    private Set<Permission> permissions = new LinkedHashSet<>();

    protected AccessRole() {
    }

    public AccessRole(Tenant tenant, String code, String name, String description, boolean systemRole) {
        this.tenant = tenant;
        this.code = code.strip().toUpperCase();
        this.name = name.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.systemRole = systemRole;
        this.status = Status.ACTIVE;
    }

    public void update(String name, String description, Set<Permission> permissions, Status status) {
        this.name = name.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.permissions.clear();
        this.permissions.addAll(permissions);
        this.status = status;
    }

    public void replacePermissions(Set<Permission> permissions) {
        this.permissions.clear();
        this.permissions.addAll(permissions);
    }

    public Tenant getTenant() { return tenant; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isSystemRole() { return systemRole; }
    public Status getStatus() { return status; }
    public Set<Permission> getPermissions() { return Set.copyOf(permissions); }
}
