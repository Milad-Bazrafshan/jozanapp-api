package ir.orix.school.card.domain;

import java.time.LocalDate;
import jakarta.persistence.*;
import ir.orix.school.classroom.domain.ClassSection;
import ir.orix.school.files.domain.StoredFile;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "student_card", schema = "school")
public class StudentCard extends AuditedEntity {
    public enum Status { ACTIVE, REVOKED }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id") private StudentProfile student;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "class_section_id") private ClassSection classSection;
    @Column(name = "valid_until", nullable = false) private LocalDate validUntil;
    @Column(name = "verification_token", nullable = false, length = 80) private String verificationToken;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "photo_file_id") private StoredFile photo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    protected StudentCard() {}
    public StudentCard(Tenant tenant, StudentProfile student, ClassSection classSection, LocalDate validUntil, String token, StoredFile photo) {
        this.tenant = tenant; this.student = student; this.classSection = classSection; this.validUntil = validUntil;
        this.verificationToken = token; this.photo = photo; this.status = Status.ACTIVE;
    }
    public Tenant getTenant() { return tenant; } public StudentProfile getStudent() { return student; }
    public ClassSection getClassSection() { return classSection; } public LocalDate getValidUntil() { return validUntil; }
    public String getVerificationToken() { return verificationToken; } public StoredFile getPhoto() { return photo; }
    public Status getStatus() { return status; }
}
