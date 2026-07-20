package akuma.whiplash.domains.place.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import akuma.whiplash.domains.place.domain.client.GoogleClient;
import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceAutocompleteCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceAutocompleteSuggestion;
import akuma.whiplash.domains.place.domain.model.PlaceDetailsCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import akuma.whiplash.domains.place.domain.model.SelectedPlaceDetail;
import akuma.whiplash.domains.place.exception.PlaceErrorCode;
import akuma.whiplash.global.exception.ApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlaceQueryServiceTest {

    private PlaceQueryServiceImpl placeQueryService;
    private GoogleClient googleClient;

    @BeforeEach
    void setUp() {
        googleClient = mock(GoogleClient.class);
        placeQueryService = new PlaceQueryServiceImpl(googleClient);
    }

    @Nested
    @DisplayName("getPlaceAutocompleteSuggestions - 장소 자동완성")
    class GetPlaceAutocompleteSuggestionsTest {

        @Test
        @DisplayName("성공: 자동완성 조건을 Google client에 전달한다")
        void success() {
            // given
            PlaceAutocompleteCriteria criteria = new PlaceAutocompleteCriteria(
                "구리", null, null, "ko", "KR", "550e8400-e29b-41d4-a716-446655440000"
            );
            List<PlaceAutocompleteSuggestion> expected = List.of(
                new PlaceAutocompleteSuggestion("구리시청", "경기도 구리시", "ChIJ")
            );
            when(googleClient.autocomplete(criteria)).thenReturn(expected);

            // when
            var result = placeQueryService.getPlaceAutocompleteSuggestions(criteria);

            // then
            assertThat(result).isEqualTo(expected);
            verify(googleClient).autocomplete(criteria);
        }
    }

    @Nested
    @DisplayName("getPlaceDetails - 선택 장소 상세 조회")
    class GetPlaceDetailsTest {

        @Test
        @DisplayName("성공: 선택 장소 조건을 Google client에 전달한다")
        void success() {
            // given
            PlaceDetailsCriteria criteria = new PlaceDetailsCriteria(
                "ChIJ", "550e8400-e29b-41d4-a716-446655440000", "ko", "KR"
            );
            SelectedPlaceDetail expected = new SelectedPlaceDetail(
                "경기도 구리시 아차산로 439", 37.5943, 127.1296, "KR", "ChIJ"
            );
            when(googleClient.getPlaceDetails(criteria)).thenReturn(expected);

            // when
            SelectedPlaceDetail result = placeQueryService.getPlaceDetails(criteria);

            // then
            assertThat(result).isEqualTo(expected);
            verify(googleClient).getPlaceDetails(criteria);
        }
    }

    @Nested
    @DisplayName("searchPlaces - 장소 검색")
    class SearchPlacesTest {

        @Test
        @DisplayName("성공: 한국 검색도 Google을 사용하고 거리를 계산한다")
        void success() {
            // given
            PlaceSearchCriteria criteria = new PlaceSearchCriteria("카페", 37.0, 127.0, 5, "ko", "KR");
            when(googleClient.searchPlaces(criteria)).thenReturn(List.of(new PlaceSearchResult(
                "카페", "서울시 강남구", 37.0, 127.0, PlaceProvider.GOOGLE, "ChIJ", "KR", null
            )));

            // when
            List<PlaceSearchResult> responses = placeQueryService.searchPlaces(criteria);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).name()).isEqualTo("카페");
            assertThat(responses.get(0).address()).isEqualTo("서울시 강남구");
            assertThat(responses.get(0).latitude()).isEqualTo(37.0);
            assertThat(responses.get(0).longitude()).isEqualTo(127.0);
            assertThat(responses.get(0).distanceMeters()).isZero();
            assertThat(responses.get(0).provider()).isEqualTo(PlaceProvider.GOOGLE);
            verify(googleClient).searchPlaces(criteria);
        }

        @Test
        @DisplayName("성공: 검색 힌트가 없어도 Google을 사용한다")
        void success_withoutHints() {
            // given
            PlaceSearchCriteria criteria = new PlaceSearchCriteria("카페", null, null, 5, null, null);
            when(googleClient.searchPlaces(criteria)).thenReturn(List.of(new PlaceSearchResult(
                "카페", "서울시 강남구", 37.0, 127.0, PlaceProvider.GOOGLE, "ChIJ", "KR", null
            )));

            // when
            List<PlaceSearchResult> responses = placeQueryService.searchPlaces(criteria);

            // then
            assertThat(responses.get(0).distanceMeters()).isNull();
            verify(googleClient).searchPlaces(criteria);
        }

        @Test
        @DisplayName("실패: 외부 API가 에러를 반환하면 예외를 던진다")
        void fail_externalApiError() {
            // given
            PlaceSearchCriteria criteria = new PlaceSearchCriteria("카페", null, null, 5, null, null);
            when(googleClient.searchPlaces(criteria))
                .thenThrow(ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR));

            // when & then
            assertThatThrownBy(() -> placeQueryService.searchPlaces(criteria))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                    assertThat(exception.getCode()).isEqualTo(PlaceErrorCode.PROVIDER_ERROR)
                );
        }
    }

    @Nested
    @DisplayName("getPlaceDetailByCoord - 장소 상세 조회")
    class GetPlaceDetailByCoordTest {

        @Test
        @DisplayName("성공: 한국 좌표와 응답 언어를 Google client에 전달한다")
        void success() {
            // given
            double latitude = 37.5665;
            double longitude = 126.9780;
            PlaceDetail expected = new PlaceDetail(
                "대한민국 서울특별시",
                "서울특별시",
                "대한민국 서울특별시",
                latitude,
                longitude,
                "KR"
            );
            when(googleClient.reverseGeocode(latitude, longitude, "ko")).thenReturn(expected);

            // when
            PlaceDetail result = placeQueryService.getPlaceDetailByCoord(latitude, longitude, "ko");

            // then
            assertThat(result).isEqualTo(expected);
            verify(googleClient).reverseGeocode(latitude, longitude, "ko");
        }
    }
}
