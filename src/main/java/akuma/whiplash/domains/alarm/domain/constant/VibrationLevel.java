package akuma.whiplash.domains.alarm.domain.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VibrationLevel {
    NONE(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    MAX(4);

    private final int level;

    public static VibrationLevel from(int level) {
        return Arrays.stream(values())
                .filter(v -> v.level == level)
                .findFirst()
                .orElse(null);
    }
}
