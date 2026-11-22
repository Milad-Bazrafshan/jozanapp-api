package ir.orix.school.assessment.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.shared.persistence.AuditedEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assessment_result", schema = "school")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentResult extends AuditedEntity {

    public enum Status {
        PENDING_GRADING,
        READY,
        PUBLISHED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id")
    private AssessmentParticipant participant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "selected_attempt_id")
    private AssessmentAttempt selectedAttempt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(name = "auto_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal autoScore;

    @Column(name = "manual_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal manualScore;

    @Column(name = "total_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "max_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private UserAccount publishedBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "balance", precision = 10, scale = 2)
    private BigDecimal balance;

    @Column(name = "rank_in_assessment")
    private Integer rankInAssessment;

    @Column(name = "participant_count")
    private Integer participantCount;

    public AssessmentResult(Tenant tenant, AssessmentParticipant participant, AssessmentAttempt attempt) {
        this.tenant = tenant;
        this.participant = participant;
        this.selectedAttempt = attempt;
        this.autoScore = BigDecimal.ZERO;
        this.manualScore = BigDecimal.ZERO;
        this.totalScore = BigDecimal.ZERO;
        this.maxScore = participant.getAssessment().getTotalPoints();
        this.status = Status.PENDING_GRADING;
    }

    public void calculate(AssessmentAttempt attempt, BigDecimal automatic, BigDecimal manual, boolean pending) {
        this.selectedAttempt = attempt;
        this.autoScore = automatic;
        this.manualScore = manual;
        this.totalScore = automatic.add(manual);
        this.maxScore = participant.getAssessment().getTotalPoints();
        if (status != Status.PUBLISHED) {
            this.status = pending ? Status.PENDING_GRADING : Status.READY;
        }
    }

    public void publish(UserAccount actor, Instant now) {
        if (status == Status.PENDING_GRADING) {
            throw new IllegalStateException("Result is not fully graded.");
        }
        if (status == Status.PUBLISHED) {
            return;
        }
        status = Status.PUBLISHED;
        publishedBy = actor;
        publishedAt = now;
    }

    /** Publishes without a human actor (e.g. auto-publish after fully auto-graded submit). */
    public void autoPublish(Instant now) {
        if (status == Status.PENDING_GRADING) {
            throw new IllegalStateException("Result is not fully graded.");
        }
        if (status == Status.PUBLISHED) {
            return;
        }
        status = Status.PUBLISHED;
        publishedBy = null;
        publishedAt = now;
    }

    public void applyRanking(BigDecimal balanceValue, int rank, int participants) {
        this.balance = balanceValue;
        this.rankInAssessment = rank;
        this.participantCount = participants;
    }
}
