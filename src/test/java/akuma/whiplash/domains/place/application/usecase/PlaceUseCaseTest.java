package akuma.whiplash.domains.place.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import akuma.whiplash.domains.place.domain.service.PlaceQueryService;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.code.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
            placeUseCase.searchPlaces(query, latitude, longitude);

            // then
            verify(placeQueryService).searchPlaces(query, latitude, longitude);
        }

        @Test
        @DisplayName("실패: query가 빈 문자열이면 예외가 발생한다")
        void fail_queryBlank() {
            // given
            String query = "";

            // when & then
            assertThatThrownBy(() -> placeUseCase.searchPlaces(query, null, null))
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
            assertThatThrownBy(() -> placeUseCase.searchPlaces(query, 37.0, null))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(CommonErrorCode.BAD_REQUEST)
                );
            verifyNoInteractions(placeQueryService);
        }
    }
}
