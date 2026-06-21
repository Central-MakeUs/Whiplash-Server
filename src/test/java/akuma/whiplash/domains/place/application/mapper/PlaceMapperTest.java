package akuma.whiplash.domains.place.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import akuma.whiplash.domains.place.application.dto.response.PlaceInfoResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlaceMapperTest {

    @Nested
    @DisplayName("mapToPlaceInfoResponses - 장소 검색 응답 변환")
    class MapToPlaceInfoResponsesTest {

        @Test
        @DisplayName("성공: provider 정보를 포함한 표준 응답으로 변환한다")
        void success() {
            // given
            PlaceSearchResult place = new PlaceSearchResult(
                "Starbucks", "New York, NY, USA", 40.7128, -74.0060,
                PlaceProvider.GOOGLE, "ChIJ", "US", 320
            );

            // when
            List<PlaceInfoResponse> result = PlaceMapper.mapToPlaceInfoResponses(List.of(place));

            // then
            assertThat(result).containsExactly(new PlaceInfoResponse(
                "Starbucks", "New York, NY, USA", 40.7128, -74.0060,
                320, "GOOGLE", "ChIJ", "US"
            ));
        }
    }

    @Nested
    @DisplayName("mapToPlaceDetailResponse - 장소 상세 응답 변환")
    class MapToPlaceDetailResponseTest {

        @Test
        @DisplayName("성공: domain 결과를 표준 장소 상세 응답으로 변환한다")
        void success() {
            // given
            PlaceDetail placeDetail = new PlaceDetail(
                "New York, NY, USA",
                "New York",
                "New York, NY, USA",
                40.7128,
                -74.0060,
                "US"
            );

            // when
            var response = PlaceMapper.mapToPlaceDetailResponse(placeDetail);

            // then
            assertThat(response.address()).isEqualTo("New York, NY, USA");
            assertThat(response.placeName()).isEqualTo("New York");
            assertThat(response.roadAddress()).isEqualTo("New York, NY, USA");
            assertThat(response.latitude()).isEqualTo(40.7128);
            assertThat(response.longitude()).isEqualTo(-74.0060);
            assertThat(response.countryCode()).isEqualTo("US");
        }
    }
}
