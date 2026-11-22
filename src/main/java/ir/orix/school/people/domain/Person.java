package ir.orix.school.people.domain;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "person", schema = "school")
public class Person extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "first_name", nullable = false, length = 120)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 120)
    private String lastName;

    @Column(name = "national_id", length = 30)
    private String nationalId;

    @Column(length = 30)
    private String mobile;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "fathers_name", length = 120)
    private String fathersName;

    @Column(name = "place_of_birth", length = 200)
    private String placeOfBirth;

    protected Person() {
    }

    public Person(Tenant tenant, String firstName, String lastName, String nationalId, String mobile,
                    LocalDate dateOfBirth, String fathersName, String placeOfBirth) {
        this.tenant = tenant;
        this.firstName = firstName.strip();
        this.lastName = lastName.strip();
        this.nationalId = normalizeNullable(nationalId);
        this.mobile = normalizeNullable(mobile);
        this.dateOfBirth = dateOfBirth;
        this.fathersName = normalizeNullable(fathersName);
        this.placeOfBirth = normalizeNullable(placeOfBirth);
    }

    public Person(Tenant tenant, String firstName, String lastName, String nationalId, String mobile) {
        this(tenant, firstName, lastName, nationalId, mobile, null, null, null);
    }

    public void update(String firstName, String lastName, String nationalId, String mobile) {
        this.firstName = firstName.strip();
        this.lastName = lastName.strip();
        this.nationalId = normalizeNullable(nationalId);
        this.mobile = normalizeNullable(mobile);
    }

    public void updateMobile(String mobile) {
        this.mobile = normalizeNullable(mobile);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public Tenant getTenant() { return tenant; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return firstName + " " + lastName; }
    public String getNationalId() { return nationalId; }
    public String getMobile() { return mobile; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getFathersName() { return fathersName; }
    public String getPlaceOfBirth() { return placeOfBirth; }
}
