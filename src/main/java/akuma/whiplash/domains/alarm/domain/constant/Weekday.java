package akuma.whiplash.domains.alarm.domain.constant;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.*;

import akuma.whiplash.domains.alarm.exception.AlarmErrorCode;
import akuma.whiplash.global.exception.ApplicationException;
import java.time.DayOfWeek;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Weekday {
    MONDAY("월", DayOfWeek.MONDAY),
    TUESDAY("화", DayOfWeek.TUESDAY),
    WEDNESDAY("수", DayOfWeek.WEDNESDAY),
    THURSDAY("목", DayOfWeek.THURSDAY),
    FRIDAY("금", DayOfWeek.FRIDAY),
    SATURDAY("토", DayOfWeek.SATURDAY),
    SUNDAY("일", DayOfWeek.SUNDAY);

    private final String description;
    private final DayOfWeek dayOfWeek;

    public static Weekday from(String description) {
        return Arrays.stream(values())
            .filter(w -> w.description.equals(description))
            .findFirst()
            .orElseThrow(() -> ApplicationException.from(INVALID_WEEKDAY));
    }

    public static Weekday from(DayOfWeek dayOfWeek) {
        return Arrays.stream(values())
            .filter(w -> w.dayOfWeek.equals(dayOfWeek))
            .findFirst()
            .orElseThrow(() -> ApplicationException.from(INVALID_WEEKDAY));
    }

    public static String getDescriptionOfDayOfWeek(DayOfWeek dayOfWeek) {
        return Arrays.stream(values())
            .filter(w -> w.dayOfWeek == dayOfWeek)
            .findFirst()
            .map(Weekday::getDescription)
            .orElseThrow(() -> ApplicationException.from(INVALID_WEEKDAY));
    }
}
