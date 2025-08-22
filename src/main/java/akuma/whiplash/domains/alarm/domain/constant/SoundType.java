package akuma.whiplash.domains.alarm.domain.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SoundType {
    NONE("소리 없음"),
    ONE("알람 소리1"),
    TWO("알람 소리2"),
    THREE("알람 소리3"),
    FOUR("알람 소리4");

    private final String description;

    public static SoundType from(String description) {
        if (description == null) {
            return NONE;
        }

        return Arrays.stream(values())
            .filter(s -> s.description.equals(description))
            .findFirst()
            .orElse(NONE);
    }
}
