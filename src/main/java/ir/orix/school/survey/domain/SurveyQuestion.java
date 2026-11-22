package ir.orix.school.survey.domain;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "survey_question", schema = "school")
public class SurveyQuestion extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "survey_id") private Survey survey;
    @Column(nullable = false, length = 400) private String prompt;
    @Column(name = "sequence_number", nullable = false) private int sequenceNumber;
    protected SurveyQuestion() {}
    public SurveyQuestion(Tenant tenant, Survey survey, String prompt, int sequenceNumber) {
        this.tenant = tenant; this.survey = survey; this.prompt = prompt.strip(); this.sequenceNumber = sequenceNumber;
    }
    public Survey getSurvey() { return survey; } public String getPrompt() { return prompt; } public int getSequenceNumber() { return sequenceNumber; }
}
