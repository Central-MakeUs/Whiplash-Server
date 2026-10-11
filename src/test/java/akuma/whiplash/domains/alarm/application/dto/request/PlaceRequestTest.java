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
    @DisplayName("place - 알람 장소 입력 검증")
    class PlaceSelectionTest {

        @Test
        @DisplayName("성공: 일반 지도 핀은 좌표만으로 등록 요청을 구성한다")
        void success_userPin() {
            // given
            PlaceRequest request = new PlaceRequest(null, 37.564213, 127.001698, null, null);

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("성공: Google 장소는 주소와 Place ID를 함께 보낸다")
        void success_googlePlace() {
            // given
            PlaceRequest request = new PlaceRequest("서울시 중구 퇴계로 24", 37.564213, 127.001698, "ChIJ", null);

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("성공: 일반 지도 핀은 역지오코딩 주소를 캐시할 수 있다")
        void success_userPinWithAddress() {
            // given
            PlaceRequest request = new PlaceRequest("서울시 중구 퇴계로 24", 37.564213, 127.001698, null, null);

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("성공: POI 이름과 좌표가 있으면 상세 주소 없이도 등록 요청을 구성한다")
        void success_googlePlaceWithoutAddress() {
            // given
            PlaceRequest request = new PlaceRequest(null, 37.564213, 127.001698, "ChIJ", null);

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("실패: 빈 Place ID를 일반 핀으로 처리하지 않는다")
        void fail_blankGooglePlaceId() {
            // given
            PlaceRequest request = new PlaceRequest("서울시 중구 퇴계로 24", 37.564213, 127.001698, " ", null);

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("실패: Google 장소에 공백 주소를 보내면 거절한다")
        void fail_googlePlaceWithBlankAddress() {
            // given
            PlaceRequest request = new PlaceRequest(" ", 37.564213, 127.001698, "ChIJ", null);

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("실패: 일반 지도 핀에는 자동완성 세션 토큰을 보낼 수 없다")
        void fail_userPinWithSessionToken() {
            // given
            PlaceRequest request = new PlaceRequest(null, 37.564213, 127.001698, null, "session-token");

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("실패: 일반 지도 핀에 좌표가 빠지면 거절한다")
        void fail_userPinWithoutLatitude() {
            // given
            PlaceRequest request = new PlaceRequest(null, null, 127.001698, null, null);

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("latitude");
        }
    }

    @Test
    @DisplayName("성공: 알람 등록 요청은 일반 지도 핀을 허용한다")
    void success_alarmRegisterRequestWithUserPin() {
        // given
        AlarmRegisterRequest request = new AlarmRegisterRequest(
            new PlaceRequest(null, 37.564213, 127.001698, null, null),
            "출근",
            LocalTime.of(8, 0),
            List.of("MONDAY"),
            "KARINA_SCOLDING"
        );

        // when
        var violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }
}
