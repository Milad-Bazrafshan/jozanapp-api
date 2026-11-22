package ir.orix.school.scheduling.application;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.*; import org.junit.jupiter.api.Test;
import ir.orix.school.scheduling.domain.ScheduleRule;

class ScheduleOccurrenceCalculatorTest {
 @Test void createsWeeklyOccurrencesFromFirstMatchingDayAtRequestedInterval(){
  var dates=ScheduleOccurrenceCalculator.dates(ScheduleRule.RecurrenceType.WEEKLY,LocalDate.of(2026,9,14),LocalDate.of(2026,10,31),DayOfWeek.SATURDAY,2,LocalDate.of(2026,9,14),LocalDate.of(2026,10,31));
  assertThat(dates).containsExactly(LocalDate.of(2026,9,19),LocalDate.of(2026,10,3),LocalDate.of(2026,10,17),LocalDate.of(2026,10,31));
 }
 @Test void onceRuleOnlyGeneratesInsideRequestedWindow(){
  var date=LocalDate.of(2026,9,20);
  assertThat(ScheduleOccurrenceCalculator.dates(ScheduleRule.RecurrenceType.ONCE,date,date,null,1,date.minusDays(1),date.plusDays(1))).containsExactly(date);
  assertThat(ScheduleOccurrenceCalculator.dates(ScheduleRule.RecurrenceType.ONCE,date,date,null,1,date.plusDays(1),date.plusDays(2))).isEmpty();
 }
}
