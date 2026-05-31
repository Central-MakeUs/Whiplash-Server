package akuma.whiplash.domains.alarm.domain.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SoundType {
    VIBRATION_ONLY("소리 없음(진동 모드)"),
    KARINA_SCOLDING("카리나의 쓴소리"),
    WAKE_AND_MOVE("일어나 움직여"),
    KIMDUHAN_WAKE_UP("일어나셔야 합니다-김두한"),
    LIFE_WARNING("인생 경고음");

    private final String description;

    public static SoundType from(String code) {
        if (code == null) {
            return VIBRATION_ONLY;
        }

        return Arrays.stream(values())
            .filter(soundType -> soundType.name().equals(code))
            .findFirst()
            .orElse(VIBRATION_ONLY);
    }
}
