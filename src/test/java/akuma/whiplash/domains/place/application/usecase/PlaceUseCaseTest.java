package akuma.whiplash.domains.place.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceSearchCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import akuma.whiplash.domains.place.exception.PlaceErrorCode;
import akuma.whiplash.domains.place.domain.service.PlaceQueryService;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.code.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.List;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceUseCaseTest {

    @Mock private PlaceQueryService placeQueryService;
    @InjectMocks private PlaceUseCase placeUseCase;

    @Nested
    @DisplayName("searchPlaces - 장소 목록 검색")
    class SearchPlacesTest {

        @Test
        @DisplayName("성공: 검색어와 현재 좌표를 서비스에 전달한다")
        void success() {
            // given
            String query = "카페";
            Double latitude = 37.0;
            Double longitude = 127.0;

            // when
            when(placeQueryService.searchPlaces(new PlaceSearchCriteria(
                query, latitude, longitude, 3, "ko", "KR"
            ))).thenReturn(List.of(new PlaceSearchResult(
                "카페", "서울", 37.0, 127.0, PlaceProvider.GOOGLE, "ChIJ", "KR", 0
            )));

            placeUseCase.searchPlaces(query, latitude, longitude, 3, "ko", "KR");

            // then
            verify(placeQueryService).searchPlaces(new PlaceSearchCriteria(
                query, latitude, longitude, 3, "ko", "KR"
            ));
        }

        @Test
        @DisplayName("실패: query가 빈 문자열이면 예외가 발생한다")
        void fail_queryBlank() {
            // given
            String query = "";

            // when & then
            assertThatThrownBy(() -> placeUseCase.searchPlaces(query, null, null, 5, null, null))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(CommonErrorCode.BAD_REQUEST)
                );
            verifyNoInteractions(placeQueryService);
        }

        @Test
        @DisplayName("실패: 현재 좌표가 하나만 전달되면 예외가 발생한다")
        void fail_partialCoordinates() {
            // given
            String query = "카페";

            // when & then
            assertThatThrownBy(() -> placeUseCase.searchPlaces(query, 37.0, null, 5, null, null))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(CommonErrorCode.BAD_REQUEST)
                );
            verifyNoInteractions(placeQueryService);
        }

        @Test
        @DisplayName("실패: size가 허용 범위를 벗어나면 예외가 발생한다")
        void fail_sizeOutOfRange() {
            // when & then
            assertThatThrownBy(() -> placeUseCase.searchPlaces("카페", null, null, 6, null, null))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(CommonErrorCode.BAD_REQUEST)
                );
            verifyNoInteractions(placeQueryService);
        }

        @Test
        @DisplayName("실패: 검색 좌표가 범위를 벗어나면 예외가 발생한다")
        void fail_coordinateOutOfRange() {
            // when & then
            assertThatThrownBy(() -> placeUseCase.searchPlaces("카페", 91.0, 127.0, 5, null, null))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(PlaceErrorCode.INVALID_COORDINATE)
                );
            verifyNoInteractions(placeQueryService);
        }

        @Test
        @DisplayName("실패: 지원하지 않는 지역 코드이면 예외가 발생한다")
        void fail_unsupportedRegion() {
            // when & then
            assertThatThrownBy(() -> placeUseCase.searchPlaces("카페", null, null, 5, null, "JP"))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(PlaceErrorCode.UNSUPPORTED_REGION)
                );
            verifyNoInteractions(placeQueryService);
        }
    }

    @Nested
    @DisplayName("getPlaceDetail - 장소 상세 조회")
    class GetPlaceDetailTest {

        @Test
        @DisplayName("성공: 좌표와 응답 언어를 서비스에 전달한다")
        void success() {
            // given
            double latitude = 40.7128;
            double longitude = -74.0060;
            String languageCode = "en";
            when(placeQueryService.getPlaceDetailByCoord(latitude, longitude, languageCode))
                .thenReturn(new PlaceDetail(
                    "New York, NY, USA",
                    "New York",
                    "New York, NY, USA",
                    latitude,
                    longitude,
                    "US"
                ));

            // when
            placeUseCase.getPlaceDetail(latitude, longitude, languageCode);

            // then
            verify(placeQueryService).getPlaceDetailByCoord(latitude, longitude, languageCode);
        }

        @Test
        @DisplayName("실패: 위도가 범위를 벗어나면 예외가 발생한다")
        void fail_latitudeOutOfRange() {
            // when & then
            assertThatThrownBy(() -> placeUseCase.getPlaceDetail(90.1, 127.0, null))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(PlaceErrorCode.INVALID_COORDINATE)
                );
            verifyNoInteractions(placeQueryService);
        }

        @Test
        @DisplayName("실패: 좌표가 유한한 값이 아니면 예외가 발생한다")
        void fail_coordinateNotFinite() {
            // when & then
            assertThatThrownBy(() -> placeUseCase.getPlaceDetail(Double.NaN, Double.POSITIVE_INFINITY, null))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(PlaceErrorCode.INVALID_COORDINATE)
                );
            verifyNoInteractions(placeQueryService);
        }

        @Test
        @DisplayName("실패: 지원하지 않는 응답 언어이면 예외가 발생한다")
        void fail_unsupportedLanguage() {
            // when & then
            assertThatThrownBy(() -> placeUseCase.getPlaceDetail(37.0, 127.0, "ja"))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(PlaceErrorCode.UNSUPPORTED_LANGUAGE)
                );
            verifyNoInteractions(placeQueryService);
        }
    }
}
