package ir.orix.school.survey.domain;

import java.time.Instant;
import jakarta.persistence.*;
import ir.orix.school.enrollment.domain.Enrollment;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "survey_response", schema = "school")
public class SurveyResponse extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "survey_id") private Survey survey;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "enrollment_id") private Enrollment enrollment;
    @Column(name = "submitted_at", nullable = false) private Instant submittedAt;
    protected SurveyResponse() {}
    public SurveyResponse(Tenant tenant, Survey survey, Enrollment enrollment, Instant submittedAt) {
        this.tenant = tenant; this.survey = survey; this.enrollment = enrollment; this.submittedAt = submittedAt;
    }
    public Survey getSurvey() { return survey; } public Enrollment getEnrollment() { return enrollment; }
}
