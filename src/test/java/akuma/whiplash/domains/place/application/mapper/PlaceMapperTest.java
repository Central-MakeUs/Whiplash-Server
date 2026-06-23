package akuma.whiplash.domains.place.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceAutocompleteSuggestion;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import akuma.whiplash.domains.place.domain.model.SelectedPlaceDetail;
import akuma.whiplash.domains.place.application.dto.response.PlaceInfoResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlaceMapperTest {

    @Nested
    @DisplayName("mapToPlaceAutocompleteResponse - 장소 자동완성 응답 변환")
    class MapToPlaceAutocompleteResponseTest {

        @Test
        @DisplayName("성공: 두 줄 추천 결과와 Google place ID를 응답으로 변환한다")
        void success() {
            // given
            PlaceAutocompleteSuggestion suggestion = new PlaceAutocompleteSuggestion(
                "구리시청", null, "ChIJ"
            );

            // when
            var response = PlaceMapper.mapToPlaceAutocompleteResponse(List.of(suggestion));

            // then
            assertThat(response.suggestions()).hasSize(1);
            assertThat(response.suggestions().get(0).mainText()).isEqualTo("구리시청");
            assertThat(response.suggestions().get(0).secondaryText()).isNull();
            assertThat(response.suggestions().get(0).providerPlaceId()).isEqualTo("ChIJ");
        }
    }

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
            assertThat(response.providerPlaceId()).isNull();
        }

        @Test
        @DisplayName("성공: 선택 장소는 장소명 없이 Google place ID를 포함해 변환한다")
        void success_selectedPlace() {
            // given
            SelectedPlaceDetail placeDetail = new SelectedPlaceDetail(
                "경기도 구리시 아차산로 439", 37.5943, 127.1296, "KR", "ChIJ"
            );

            // when
            var response = PlaceMapper.mapToPlaceDetailResponse(placeDetail);

            // then
            assertThat(response.address()).isEqualTo("경기도 구리시 아차산로 439");
            assertThat(response.placeName()).isNull();
            assertThat(response.roadAddress()).isEqualTo("경기도 구리시 아차산로 439");
            assertThat(response.latitude()).isEqualTo(37.5943);
            assertThat(response.longitude()).isEqualTo(127.1296);
            assertThat(response.countryCode()).isEqualTo("KR");
            assertThat(response.providerPlaceId()).isEqualTo("ChIJ");
        }
    }
}
