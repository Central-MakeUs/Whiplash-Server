package akuma.whiplash.domains.alarm.domain.constant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("SoundType - 소리 유형 변환")
class SoundTypeTest {

    @ParameterizedTest
    @CsvSource({
        "VIBRATION_ONLY,VIBRATION_ONLY",
        "KARINA_SCOLDING,KARINA_SCOLDING",
        "WAKE_AND_MOVE,WAKE_AND_MOVE",
        "KIMDUHAN_WAKE_UP,KIMDUHAN_WAKE_UP",
        "LIFE_WARNING,LIFE_WARNING"
    })
    @DisplayName("코드와 일치하는 소리 유형을 반환한다")
    void returnsMatchingSoundType(String code, SoundType expected) {
        // given

        // when
        SoundType soundType = SoundType.from(code);

        // then
        assertThat(soundType).isEqualTo(expected);
    }

    @Test
    @DisplayName("존재하지 않는 코드를 입력하면 진동 모드로 매핑된다")
    void returnsVibrationOnlyWhenCodeInvalid() {
        // given
        String code = "INVALID_SOUND";

        // when
        SoundType soundType = SoundType.from(code);

        // then
        assertThat(soundType).isEqualTo(SoundType.VIBRATION_ONLY);
    }

    @Test
    @DisplayName("null을 입력하면 진동 모드로 매핑된다")
    void returnsVibrationOnlyWhenCodeIsNull() {
        // given
        String code = null;

        // when
        SoundType soundType = SoundType.from(code);

        // then
        assertThat(soundType).isEqualTo(SoundType.VIBRATION_ONLY);
    }
}
