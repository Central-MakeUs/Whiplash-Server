package akuma.whiplash.domains.alarm.application.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("알람 장소 요청을 검증한다")
class PlaceRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Nested
    @DisplayName("Google Place ID를 검증한다")
    class GooglePlaceIdTest {

        @Test
        @DisplayName("실패: Google Place ID 없이 알람 장소를 등록할 수 없다")
        void fail_googlePlaceIdMissing() {
            // given
            PlaceRequest request = new PlaceRequest("서울시 중구 퇴계로 24", 37.564213, 127.001698, "", null);

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("googlePlaceId");
        }
    }

    @Test
    @DisplayName("실패: 알람 등록 요청은 중첩된 Google Place ID를 검증한다")
    void fail_alarmRegisterRequestWithBlankGooglePlaceId() {
        // given
        AlarmRegisterRequest request = new AlarmRegisterRequest(
            new PlaceRequest("서울시 중구 퇴계로 24", 37.564213, 127.001698, "", null),
            "출근",
            LocalTime.of(8, 0),
            List.of("MONDAY"),
            "KARINA_SCOLDING"
        );

        // when
        var violations = validator.validate(request);

        // then
        assertThat(violations)
            .extracting(violation -> violation.getPropertyPath().toString())
            .contains("place.googlePlaceId");
    }
}
