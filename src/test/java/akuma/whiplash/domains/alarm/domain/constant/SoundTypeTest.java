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
        "소리 없음,NONE",
        "알람 소리1,ONE",
        "알람 소리2,TWO",
        "알람 소리3,THREE",
        "알람 소리4,FOUR"
    })
    @DisplayName("설명과 일치하는 소리 유형을 반환한다")
    void returnsMatchingSoundType(String description, SoundType expected) {
        // given

        // when
        SoundType soundType = SoundType.from(description);

        // then
        assertThat(soundType).isEqualTo(expected);
    }

    @Test
    @DisplayName("존재하지 않는 설명을 입력하면 NONE으로 매핑된다")
    void returnsNoneWhenDescriptionInvalid() {
        // given
        String description = "잘못된 소리";

        // when
        SoundType soundType = SoundType.from(description);

        // then
        assertThat(soundType).isEqualTo(SoundType.NONE);
    }

    @Test
    @DisplayName("null을 입력하면 NONE으로 매핑된다")
    void returnsNoneWhenDescriptionIsNull() {
        // given
        String description = null;

        // when
        SoundType soundType = SoundType.from(description);

        // then
        assertThat(soundType).isEqualTo(SoundType.NONE);
    }
}
