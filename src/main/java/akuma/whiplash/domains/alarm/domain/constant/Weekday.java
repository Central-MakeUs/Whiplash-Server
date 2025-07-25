package akuma.whiplash.domains.alarm.domain.constant;

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
            .orElse(null);
    }

}
