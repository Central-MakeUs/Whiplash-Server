package akuma.whiplash.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GeoUtilsTest {

    @Nested
    @DisplayName("calculateDistanceMeters - 좌표 간 거리 계산")
    class CalculateDistanceMetersTest {

        @Test
        @DisplayName("성공: 알려진 두 좌표 간 거리를 미터 단위로 계산한다")
        void success() {
            // given
            double seoulStationLatitude = 37.5547;
            double seoulStationLongitude = 126.9706;
            double cityHallLatitude = 37.5663;
            double cityHallLongitude = 126.9779;

            // when
            int distanceMeters = GeoUtils.calculateDistanceMeters(
                seoulStationLatitude,
                seoulStationLongitude,
                cityHallLatitude,
                cityHallLongitude
            );

            // then
            assertThat(distanceMeters).isBetween(1_400, 1_500);
        }

        @Test
        @DisplayName("성공: 동일 좌표는 0m를 반환한다")
        void success_samePoint() {
            // given
            double latitude = 37.5547;
            double longitude = 126.9706;

            // when
            int distanceMeters = GeoUtils.calculateDistanceMeters(latitude, longitude, latitude, longitude);

            // then
            assertThat(distanceMeters).isZero();
        }
    }
}
