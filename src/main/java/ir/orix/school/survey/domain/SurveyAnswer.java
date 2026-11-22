package ir.orix.school.survey.domain;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "survey_answer", schema = "school")
public class SurveyAnswer extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "response_id") private SurveyResponse response;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "question_id") private SurveyQuestion question;
    @Column(nullable = false) private int score;
    @Column(length = 500) private String comment;
    protected SurveyAnswer() {}
    public SurveyAnswer(Tenant tenant, SurveyResponse response, SurveyQuestion question, int score, String comment) {
        this.tenant = tenant; this.response = response; this.question = question; this.score = score; this.comment = comment;
    }
    public int getScore() { return score; } public String getComment() { return comment; } public SurveyQuestion getQuestion() { return question; }
}
