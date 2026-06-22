package akuma.whiplash.domains.place.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import akuma.whiplash.domains.place.domain.client.NaverClient;
import akuma.whiplash.domains.place.domain.client.GoogleClient;
import akuma.whiplash.domains.place.domain.client.dto.NaverLocalSearchResponse;
import akuma.whiplash.domains.place.domain.client.dto.NaverLocalSearchResponse.Item;
import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceSearchCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import akuma.whiplash.domains.place.exception.PlaceErrorCode;
import akuma.whiplash.global.exception.ApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlaceQueryServiceTest {

    private PlaceQueryServiceImpl placeQueryService;
    private NaverClient naverClient;
    private GoogleClient googleClient;

    @BeforeEach
    void setUp() {
        naverClient = mock(NaverClient.class);
        googleClient = mock(GoogleClient.class);
        placeQueryService = new PlaceQueryServiceImpl(naverClient, googleClient);
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
    @DisplayName("searchPlaceKeywords - 연관 장소 키워드 조회")
    class SearchPlaceKeywordsTest {

        @Test
        @DisplayName("성공: Naver 검색 결과에서 주소 키워드를 추출한다")
        void success() {
            // given
            when(naverClient.searchLocal("강남")).thenReturn(new NaverLocalSearchResponse(List.of(
                new Item("강남역", "서울시 강남구 역삼동", "서울시 강남구 강남대로", "1270000000", "370000000")
            )));

            // when
            List<String> result = placeQueryService.searchPlaceKeywords("강남");

            // then
            assertThat(result).containsExactly("서울시 강남구 역삼동", "서울시 강남구 강남대로");
            verify(naverClient).searchLocal("강남");
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
