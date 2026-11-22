package ir.orix.school.organization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "branch", schema = "school")
public class Branch extends AuditedEntity {

    public enum Status { ACTIVE, INACTIVE }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private EducationalInstitution institution;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(length = 30)
    private String phone;

    @Column(length = 1000)
    private String address;

    protected Branch() {
    }

    public Branch(Tenant tenant, EducationalInstitution institution, String code, String name, String phone, String address) {
        this.tenant = tenant;
        this.institution = institution;
        this.code = code.strip().toUpperCase();
        this.name = name.strip();
        this.phone = normalizeNullable(phone);
        this.address = normalizeNullable(address);
        this.status = Status.ACTIVE;
    }

    public void update(String name, String phone, String address, Status status) {
        this.name = name.strip();
        this.phone = normalizeNullable(phone);
        this.address = normalizeNullable(address);
        this.status = status;
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public Tenant getTenant() { return tenant; }
    public EducationalInstitution getInstitution() { return institution; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Status getStatus() { return status; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
}
