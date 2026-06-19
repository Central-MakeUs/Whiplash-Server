package akuma.whiplash.domains.place.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import akuma.whiplash.domains.place.application.dto.response.PlaceInfoResponse;
import akuma.whiplash.domains.place.domain.client.NaverClient;
import akuma.whiplash.domains.place.domain.client.dto.NaverLocalSearchResponse;
import akuma.whiplash.domains.place.domain.client.dto.NaverLocalSearchResponse.Item;
import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.exception.PlaceErrorCode;
import akuma.whiplash.global.exception.ApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlaceQueryServiceTest {

    private PlaceQueryServiceImpl placeQueryService;
    private PlaceProviderRouter placeProviderRouter;
    private NaverClient naverClient;

    @BeforeEach
    void setUp() {
        naverClient = mock(NaverClient.class);
        placeProviderRouter = mock(PlaceProviderRouter.class);
        placeQueryService = new PlaceQueryServiceImpl(naverClient, placeProviderRouter);
    }

    @Nested
    @DisplayName("searchPlaces - 장소 검색")
    class SearchPlacesTest {

        @Test
        @DisplayName("성공: 키워드 검색 결과를 반환한다")
        void success() {
            // given
            when(naverClient.searchLocal("카페")).thenReturn(new NaverLocalSearchResponse(List.of(
                new Item("<b>카페</b>", "서울시 강남구 역삼동", "서울시 강남구", "1270000000", "370000000")
            )));

            // when
            List<PlaceInfoResponse> responses = placeQueryService.searchPlaces("카페", null, null);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).name()).isEqualTo("카페");
            assertThat(responses.get(0).address()).isEqualTo("서울시 강남구");
            assertThat(responses.get(0).latitude()).isEqualTo(37.0);
            assertThat(responses.get(0).longitude()).isEqualTo(127.0);
            verify(naverClient).searchLocal("카페");
        }

        @Test
        @DisplayName("실패: 외부 API가 에러를 반환하면 예외를 던진다")
        void fail_externalApiError() {
            // given
            when(naverClient.searchLocal("카페"))
                .thenThrow(ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR));

            // when & then
            assertThatThrownBy(() -> placeQueryService.searchPlaces("카페", null, null))
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
        @DisplayName("성공: 좌표와 응답 언어를 provider router에 전달한다")
        void success() {
            // given
            PlaceDetail expected = new PlaceDetail(
                "New York, NY, USA",
                "New York",
                "New York, NY, USA",
                40.7128,
                -74.0060,
                "US",
                PlaceProvider.GOOGLE
            );
            when(placeProviderRouter.getPlaceDetail(40.7128, -74.0060, "en")).thenReturn(expected);

            // when
            PlaceDetail result = placeQueryService.getPlaceDetailByCoord(40.7128, -74.0060, "en");

            // then
            assertThat(result).isEqualTo(expected);
            verify(placeProviderRouter).getPlaceDetail(40.7128, -74.0060, "en");
        }
    }
}
