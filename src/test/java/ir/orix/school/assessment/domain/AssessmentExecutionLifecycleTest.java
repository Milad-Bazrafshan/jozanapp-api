/*
package ir.orix.school.assessment.domain;

import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;
import java.math.BigDecimal; import java.time.Instant;
import org.junit.jupiter.api.Test;
import ir.orix.school.identity.domain.UserAccount; import ir.orix.school.organization.domain.Tenant;

class AssessmentExecutionLifecycleTest {
 @Test void attemptSubmissionIsIdempotentAndReadyResultCanBePublished(){Tenant tenant=mock(Tenant.class);Assessment assessment=mock(Assessment.class);AssessmentParticipant participant=mock(AssessmentParticipant.class);UserAccount actor=mock(UserAccount.class);when(participant.getAssessment()).thenReturn(assessment);when(assessment.getTotalPoints()).thenReturn(new BigDecimal("20"));Instant start=Instant.parse("2026-09-15T10:00:00Z");AssessmentAttempt attempt=new AssessmentAttempt(tenant,participant,1,start,start.plusSeconds(3600));attempt.submit(false,start.plusSeconds(1200));attempt.submit(true,start.plusSeconds(1300));assertEquals(AssessmentAttempt.Status.SUBMITTED,attempt.getStatus());assertEquals(start.plusSeconds(1200),attempt.getSubmittedAt());AssessmentResult result=new AssessmentResult(tenant,participant,attempt);result.calculate(attempt,new BigDecimal("12"),new BigDecimal("6"),false);result.publish(actor,start.plusSeconds(1400));assertEquals(AssessmentResult.Status.PUBLISHED,result.getStatus());assertEquals(new BigDecimal("18"),result.getTotalScore());}
 @Test void pendingManualGradeCannotBePublished(){Tenant tenant=mock(Tenant.class);Assessment assessment=mock(Assessment.class);AssessmentParticipant participant=mock(AssessmentParticipant.class);when(participant.getAssessment()).thenReturn(assessment);when(assessment.getTotalPoints()).thenReturn(BigDecimal.TEN);AssessmentAttempt attempt=new AssessmentAttempt(tenant,participant,1,Instant.EPOCH,Instant.EPOCH.plusSeconds(60));AssessmentResult result=new AssessmentResult(tenant,participant,attempt);result.calculate(attempt,BigDecimal.ONE,BigDecimal.ZERO,true);assertThrows(IllegalStateException.class,()->result.publish(mock(UserAccount.class),Instant.EPOCH));}
}
*/
