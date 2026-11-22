package ir.orix.school.assessment.domain;

import java.time.Instant;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name="assessment_attempt",schema="school")
public class AssessmentAttempt extends AuditedEntity {
 public enum Status { IN_PROGRESS, SUBMITTED, AUTO_SUBMITTED }
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="participant_id") private AssessmentParticipant participant;
 @Column(name="attempt_number",nullable=false) private int attemptNumber;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
 @Column(name="started_at",nullable=false) private Instant startedAt;
 @Column(name="deadline_at",nullable=false) private Instant deadlineAt;
 @Column(name="submitted_at") private Instant submittedAt;
 protected AssessmentAttempt(){}
 public AssessmentAttempt(Tenant tenant,AssessmentParticipant participant,int number,Instant startedAt,Instant deadlineAt){this.tenant=tenant;this.participant=participant;this.attemptNumber=number;this.startedAt=startedAt;this.deadlineAt=deadlineAt;this.status=Status.IN_PROGRESS;}
 public void submit(boolean automatic,Instant now){if(status!=Status.IN_PROGRESS)return;status=automatic?Status.AUTO_SUBMITTED:Status.SUBMITTED;submittedAt=now;}
 public AssessmentParticipant getParticipant(){return participant;} public int getAttemptNumber(){return attemptNumber;} public Status getStatus(){return status;} public Instant getStartedAt(){return startedAt;} public Instant getDeadlineAt(){return deadlineAt;} public Instant getSubmittedAt(){return submittedAt;}
}
