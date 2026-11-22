package ir.orix.school.survey.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import ir.orix.school.classroom.domain.ClassSection;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.people.domain.InstructorProfile;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "survey", schema = "school")
public class Survey extends AuditedEntity {
    public enum Audience { GUARDIANS, STUDENTS, BOTH }
    public enum Status { DRAFT, OPEN, CLOSED }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "instructor_id") private InstructorProfile instructor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "class_section_id") private ClassSection classSection;
    @Column(nullable = false, length = 200) private String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private Audience audience;
    @Column(nullable = false) private boolean anonymous = true;
    @Column(name = "opens_at", nullable = false) private Instant opensAt;
    @Column(name = "closes_at", nullable = false) private Instant closesAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("sequenceNumber")
    private List<SurveyQuestion> questions = new ArrayList<>();
    protected Survey() {}
    public Survey(Tenant tenant, InstructorProfile instructor, ClassSection section, String title, Audience audience, Instant opensAt, Instant closesAt) {
        this.tenant = tenant; this.instructor = instructor; this.classSection = section; this.title = title.strip();
        this.audience = audience; this.opensAt = opensAt; this.closesAt = closesAt; this.status = Status.OPEN; this.anonymous = true;
    }
    public void addQuestion(String prompt, int sequence) { questions.add(new SurveyQuestion(tenant, this, prompt, sequence)); }
    public Tenant getTenant() { return tenant; } public InstructorProfile getInstructor() { return instructor; }
    public ClassSection getClassSection() { return classSection; } public String getTitle() { return title; }
    public Audience getAudience() { return audience; } public Instant getOpensAt() { return opensAt; }
    public Instant getClosesAt() { return closesAt; } public Status getStatus() { return status; } public List<SurveyQuestion> getQuestions() { return questions; }
}
