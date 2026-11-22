package ir.orix.school.people.domain;

import java.time.LocalDate;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "student_profile", schema = "school")
public class StudentProfile extends AuditedEntity {
    public enum Status {ACTIVE, SUSPENDED, GRADUATED, WITHDRAWN}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id")
    private Person person;
    @Column(name = "student_code", nullable = false, length = 50)
    private String studentCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;
    @Column(name = "admission_date")
    private LocalDate admissionDate;

    protected StudentProfile() {
    }

    public StudentProfile(Tenant tenant, Person person, String code, LocalDate admissionDate) {
        this.tenant = tenant;
        this.person = person;
        this.studentCode = code.strip().toUpperCase();
        this.admissionDate = admissionDate;
        this.status = Status.ACTIVE;
    }

    public void update(Status status, LocalDate admissionDate) {
        this.status = status;
        this.admissionDate = admissionDate;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Person getPerson() {
        return person;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }
}
