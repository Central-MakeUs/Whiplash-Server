package akuma.whiplash.domains.alarm.domain.util;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.REPEAT_DAYS_NOT_CONFIG;

import akuma.whiplash.global.exception.ApplicationException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

public class AlarmScheduleCalculator {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

    private AlarmScheduleCalculator() {
        throw new IllegalArgumentException();
    }

    public static LocalDate getNextOccurrenceDate(
        Set<DayOfWeek> repeatDays,
        ZonedDateTime fromDateTime,
        LocalTime alarmTime
    ) {
        LocalDate today = fromDateTime.toLocalDate();
        LocalTime now = fromDateTime.toLocalTime();

        if (repeatDays.contains(today.getDayOfWeek()) && alarmTime.isAfter(now)) {
            return today;
        }

        return getNextOccurrenceDate(repeatDays, today.plusDays(1));
    }

    public static LocalDate getNextOccurrenceDate(Set<DayOfWeek> repeatDays, LocalDate fromDate) {
        for (int i = 0; i < 7; i++) {
            LocalDate candidate = fromDate.plusDays(i);
            if (repeatDays.contains(candidate.getDayOfWeek())) {
                return candidate;
            }
        }

        throw ApplicationException.from(REPEAT_DAYS_NOT_CONFIG);
    }

    public static LocalDateTime toDefaultZoneLocalDateTime(
        LocalDate occurrenceDate,
        LocalTime occurrenceTime,
        ZoneId memberZone
    ) {
        return toZonedDateTime(occurrenceDate, occurrenceTime, memberZone)
            .withZoneSameInstant(DEFAULT_ZONE)
            .toLocalDateTime();
    }

    public static long toEpochMillis(
        LocalDate occurrenceDate,
        LocalTime occurrenceTime,
        ZoneId memberZone
    ) {
        return toInstant(occurrenceDate, occurrenceTime, memberZone).toEpochMilli();
    }

    public static boolean isDue(
        LocalDate occurrenceDate,
        LocalTime occurrenceTime,
        ZoneId memberZone,
        Instant now
    ) {
        return !now.isBefore(toInstant(occurrenceDate, occurrenceTime, memberZone));
    }

    public static ZoneId resolveZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return DEFAULT_ZONE;
        }
        return ZoneId.of(timeZone);
    }

    private static Instant toInstant(
        LocalDate occurrenceDate,
        LocalTime occurrenceTime,
        ZoneId memberZone
    ) {
        return toZonedDateTime(occurrenceDate, occurrenceTime, memberZone).toInstant();
    }

    private static ZonedDateTime toZonedDateTime(
        LocalDate occurrenceDate,
        LocalTime occurrenceTime,
        ZoneId memberZone
    ) {
        return LocalDateTime.of(occurrenceDate, occurrenceTime).atZone(memberZone);
    }
}
