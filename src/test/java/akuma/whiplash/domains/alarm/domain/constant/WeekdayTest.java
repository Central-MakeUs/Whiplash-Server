package akuma.whiplash.domains.alarm.domain.constant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import akuma.whiplash.domains.alarm.exception.AlarmErrorCode;
import akuma.whiplash.global.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Weekday - 요일 변환")
class WeekdayTest {

    @ParameterizedTest
    @CsvSource({
        "MONDAY,MONDAY",
        "TUESDAY,TUESDAY",
        "WEDNESDAY,WEDNESDAY",
        "THURSDAY,THURSDAY",
        "FRIDAY,FRIDAY",
        "SATURDAY,SATURDAY",
        "SUNDAY,SUNDAY"
    })
    @DisplayName("코드와 일치하는 요일을 반환한다")
    void returnsMatchingWeekday(String code, Weekday expected) {
        // given

        // when
        Weekday weekday = Weekday.from(code);

        // then
        assertThat(weekday).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "월",
        "INVALID_WEEKDAY"
    })
    @DisplayName("존재하지 않는 코드를 입력하면 예외가 발생한다")
    void throwsExceptionWhenCodeInvalid(String code) {
        // given

        // when & then
        assertThatThrownBy(() -> Weekday.from(code))
            .isInstanceOf(ApplicationException.class)
            .extracting("code")
            .isEqualTo(AlarmErrorCode.INVALID_WEEKDAY);
    }
}
