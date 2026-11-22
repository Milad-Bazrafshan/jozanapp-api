package ir.orix.school.assessment.domain;

import java.math.BigDecimal; import java.time.Instant;
import jakarta.persistence.*;
import ir.orix.school.identity.domain.UserAccount; import ir.orix.school.organization.domain.Tenant; import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name="student_response",schema="school")
public class StudentResponse extends AuditedEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="attempt_id") private AssessmentAttempt attempt;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="assessment_question_id") private AssessmentQuestion assessmentQuestion;
 @Column(name="answer_json",nullable=false,columnDefinition="TEXT") private String answerJson;
 @Column(name="saved_at",nullable=false) private Instant savedAt;
 @Column(name="auto_score",precision=10,scale=2) private BigDecimal autoScore;
 @Column(name="manual_score",precision=10,scale=2) private BigDecimal manualScore;
 @Column(columnDefinition="TEXT") private String feedback;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="graded_by") private UserAccount gradedBy;
 @Column(name="graded_at") private Instant gradedAt;
 protected StudentResponse(){}
 public StudentResponse(Tenant tenant,AssessmentAttempt attempt,AssessmentQuestion question,String answerJson,Instant now){this.tenant=tenant;this.attempt=attempt;this.assessmentQuestion=question;save(answerJson,now);}
 public void save(String answerJson,Instant now){this.answerJson=answerJson;this.savedAt=now;}
 public void autoScore(BigDecimal score){this.autoScore=score;}
 public void grade(BigDecimal score,String feedback,UserAccount actor,Instant now){this.manualScore=score;this.feedback=feedback==null||feedback.isBlank()?null:feedback.strip();this.gradedBy=actor;this.gradedAt=now;}
 public AssessmentAttempt getAttempt(){return attempt;} public AssessmentQuestion getAssessmentQuestion(){return assessmentQuestion;} public String getAnswerJson(){return answerJson;} public Instant getSavedAt(){return savedAt;} public BigDecimal getAutoScore(){return autoScore;} public BigDecimal getManualScore(){return manualScore;} public String getFeedback(){return feedback;} public Instant getGradedAt(){return gradedAt;}
}
