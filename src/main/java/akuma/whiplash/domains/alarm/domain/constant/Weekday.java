package akuma.whiplash.domains.alarm.domain.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Weekday {
    MONDAY("월"),
    TUESDAY("화"),
    WEDNESDAY("수"),
    THURSDAY("목"),
    FRIDAY("금"),
    SATURDAY("토"),
    SUNDAY("일");

    private final String description;

    public static Weekday from(String description) {
        return Arrays.stream(values())
            .filter(w -> w.description.equals(description))
            .findFirst()
            .orElse(null);
    }

}
