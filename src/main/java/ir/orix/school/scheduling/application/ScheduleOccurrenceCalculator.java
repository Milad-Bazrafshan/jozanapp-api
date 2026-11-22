package ir.orix.school.scheduling.application;

import java.time.*;
import java.time.temporal.*;
import java.util.*;

import ir.orix.school.scheduling.domain.ScheduleRule;

public final class ScheduleOccurrenceCalculator {
    private ScheduleOccurrenceCalculator() {
    }

    public static List<LocalDate> dates(ScheduleRule rule, LocalDate requestedFrom, LocalDate requestedTo) {
        return dates(rule.getRecurrenceType(), rule.getStartsOn(), rule.getEndsOn(), rule.getDayOfWeek(), rule.getIntervalWeeks(), requestedFrom, requestedTo);
    }

    public static List<LocalDate> dates(ScheduleRule.RecurrenceType recurrence, LocalDate startsOn, LocalDate endsOn, DayOfWeek day, int interval, LocalDate requestedFrom, LocalDate requestedTo) {
        LocalDate from = startsOn.isAfter(requestedFrom) ? startsOn : requestedFrom;
        LocalDate to = endsOn.isBefore(requestedTo) ? endsOn : requestedTo;
        if (to.isBefore(from)) return List.of();
        if (ChronoUnit.DAYS.between(from, to) > 366)
            throw new IllegalArgumentException("Generation range cannot exceed 366 days.");
        if (recurrence == ScheduleRule.RecurrenceType.ONCE)
            return !startsOn.isBefore(from) && !startsOn.isAfter(to) ? List.of(startsOn) : List.of();
        LocalDate anchor = startsOn.with(TemporalAdjusters.nextOrSame(day));
        LocalDate cursor = from.with(TemporalAdjusters.nextOrSame(day));
        List<LocalDate> result = new ArrayList<>();
        while (!cursor.isAfter(to)) {
            long weeks = ChronoUnit.WEEKS.between(anchor, cursor);
            if (weeks >= 0 && weeks % interval == 0) result.add(cursor);
            cursor = cursor.plusWeeks(1);
        }
        return result;
    }
}
