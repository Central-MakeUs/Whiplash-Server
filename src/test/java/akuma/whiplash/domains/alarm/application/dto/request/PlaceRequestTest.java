package akuma.whiplash.domains.alarm.application.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PlaceRequest Unit Test")
class PlaceRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Nested
    @DisplayName("googlePlaceId - Google 장소 식별자")
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
}
