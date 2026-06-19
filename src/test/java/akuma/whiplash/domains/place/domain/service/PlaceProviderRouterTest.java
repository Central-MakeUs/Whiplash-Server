package akuma.whiplash.domains.place.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import akuma.whiplash.domains.place.domain.client.GoogleClient;
import akuma.whiplash.domains.place.domain.client.NaverClient;
import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
import akuma.whiplash.domains.place.domain.model.PlaceSearchCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import akuma.whiplash.domains.place.exception.PlaceErrorCode;
import akuma.whiplash.global.exception.ApplicationException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceProviderRouterTest {

    @Mock private NaverClient naverClient;
    @Mock private GoogleClient googleClient;
    @InjectMocks private PlaceProviderRouter placeProviderRouter;

    @Nested
    @DisplayName("searchPlaces - 검색 provider routing")
    class SearchPlacesTest {

        @Test
        @DisplayName("성공: regionCode가 좌표와 충돌하면 regionCode를 우선한다")
        void success_regionCodePriority() {
            // given
            PlaceSearchCriteria criteria = new PlaceSearchCriteria(
                "Starbucks", 37.5665, 126.9780, 5, "en", "US"
            );
            List<PlaceSearchResult> expected = List.of(globalResult());
            when(googleClient.searchPlaces("Starbucks", 5, "en", "US")).thenReturn(expected);

            // when
            List<PlaceSearchResult> result = placeProviderRouter.searchPlaces(criteria);

            // then
            assertThat(result).isEqualTo(expected);
            verify(googleClient).searchPlaces("Starbucks", 5, "en", "US");
            verifyNoInteractions(naverClient);
        }

        @Test
        @DisplayName("성공: regionCode가 없고 한국 좌표이면 Naver를 사용한다")
        void success_koreaCoordinate() {
            // given
            PlaceSearchCriteria criteria = new PlaceSearchCriteria(
                "카페", 37.5665, 126.9780, 3, "ko", null
            );
            List<PlaceSearchResult> expected = List.of(koreaResult());
            when(naverClient.searchPlaces("카페", 3)).thenReturn(expected);

            // when
            List<PlaceSearchResult> result = placeProviderRouter.searchPlaces(criteria);

            // then
            assertThat(result).isEqualTo(expected);
            verify(naverClient).searchPlaces("카페", 3);
            verifyNoInteractions(googleClient);
        }

        @Test
        @DisplayName("성공: 검색 힌트가 없으면 기존 동작대로 Naver를 사용한다")
        void success_defaultNaver() {
            // given
            PlaceSearchCriteria criteria = new PlaceSearchCriteria("카페", null, null, 5, null, null);
            when(naverClient.searchPlaces("카페", 5)).thenReturn(List.of(koreaResult()));

            // when
            placeProviderRouter.searchPlaces(criteria);

            // then
            verify(naverClient).searchPlaces("카페", 5);
            verifyNoInteractions(googleClient);
        }

        @Test
        @DisplayName("실패: Naver 검색 결과가 없으면 Google fallback 없이 예외를 던진다")
        void fail_naverResultEmpty() {
            // given
            PlaceSearchCriteria criteria = new PlaceSearchCriteria("카페", null, null, 5, null, "KR");
            when(naverClient.searchPlaces("카페", 5)).thenReturn(List.of());

            // when & then
            assertThatThrownBy(() -> placeProviderRouter.searchPlaces(criteria))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                    assertThat(exception.getCode()).isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND)
                );
            verifyNoInteractions(googleClient);
        }

        private PlaceSearchResult koreaResult() {
            return new PlaceSearchResult(
                "카페", "서울시 강남구", 37.0, 127.0,
                PlaceProvider.NAVER, null, "KR", null
            );
        }

        private PlaceSearchResult globalResult() {
            return new PlaceSearchResult(
                "Starbucks", "New York, NY, USA", 40.7128, -74.0060,
                PlaceProvider.GOOGLE, "ChIJ", "US", null
            );
        }
    }

    @Nested
    @DisplayName("getPlaceDetail - 좌표 기반 provider routing")
    class GetPlaceDetailTest {

        @Test
        @DisplayName("성공: 한국 bbox 내부 좌표는 Naver로 조회한다")
        void success_koreaCoordinate() {
            // given
            double latitude = 37.5665;
            double longitude = 126.9780;

            // when
            placeProviderRouter.getPlaceDetail(latitude, longitude, "ko");

            // then
            verify(naverClient).reverseGeocode(latitude, longitude);
            verifyNoInteractions(googleClient);
        }

        @Test
        @DisplayName("성공: 한국 bbox 경계 좌표는 Naver로 조회한다")
        void success_koreaBoundaryCoordinate() {
            // when
            placeProviderRouter.getPlaceDetail(33.0, 132.0, null);

            // then
            verify(naverClient).reverseGeocode(33.0, 132.0);
            verifyNoInteractions(googleClient);
        }

        @Test
        @DisplayName("성공: 한국 bbox 외부 좌표는 Google로 조회한다")
        void success_globalCoordinate() {
            // given
            double latitude = 40.7128;
            double longitude = -74.0060;

            // when
            placeProviderRouter.getPlaceDetail(latitude, longitude, "en");

            // then
            verify(googleClient).reverseGeocode(latitude, longitude, "en");
            verifyNoInteractions(naverClient);
        }

        @Test
        @DisplayName("실패: Naver 조회가 실패해도 Google로 fallback하지 않는다")
        void fail_naverWithoutGoogleFallback() {
            // given
            when(naverClient.reverseGeocode(37.5665, 126.9780))
                .thenThrow(ApplicationException.from(PlaceErrorCode.PLACE_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> placeProviderRouter.getPlaceDetail(37.5665, 126.9780, null))
                .isInstanceOf(ApplicationException.class);
            verifyNoInteractions(googleClient);
        }
    }
}
