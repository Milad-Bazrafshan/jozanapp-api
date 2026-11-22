package ir.orix.school.assessment.domain;
import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.mock; import java.math.BigDecimal; import java.time.Instant; import org.junit.jupiter.api.Test; import ir.orix.school.identity.domain.UserAccount; import ir.orix.school.organization.domain.Tenant;
class AssessmentPublicationTest {
 @Test void publicationLocksAssessmentStructure(){
  Assessment value=new Assessment(mock(Tenant.class),null,"EX-1","آزمون فصل اول",Assessment.Type.EXAM,Instant.parse("2026-09-20T08:00:00Z"),Instant.parse("2026-09-20T10:00:00Z"),60,1,false,0,mock(UserAccount.class));
  value.updateTotalPoints(new BigDecimal("20.00"));value.publish(mock(UserAccount.class),Instant.parse("2026-09-19T08:00:00Z"));
  assertThat(value.getStatus()).isEqualTo(Assessment.Status.PUBLISHED);assertThat(value.getTotalPoints()).isEqualByComparingTo("20.00");assertThatThrownBy(()->value.updateTotalPoints(BigDecimal.TEN)).isInstanceOf(IllegalStateException.class);
 }
}
