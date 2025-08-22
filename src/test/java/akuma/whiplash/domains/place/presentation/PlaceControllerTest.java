package akuma.whiplash.domains.place.presentation;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.domains.place.application.dto.response.PlaceInfoResponse;
import akuma.whiplash.domains.place.application.usecase.PlaceUseCase;
import akuma.whiplash.global.config.security.SecurityConfig;
import akuma.whiplash.global.config.security.jwt.JwtAuthenticationFilter;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.code.CommonErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = PlaceController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            SecurityConfig.class,
            JwtAuthenticationFilter.class
        })
    }
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PlaceController Slice Test")
class PlaceControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private PlaceUseCase placeUseCase;

    private static final String BASE = "/api/places";

    @Nested
    @DisplayName("[GET] /api/places/search - 장소 목록 검색")
    class SearchPlacesTest {

        @Test
        @DisplayName("성공: 200 OK와 장소 목록을 반환한다")
        void success() throws Exception {
            // given
            List<PlaceInfoResponse> responses = List.of(
                PlaceInfoResponse.builder()
                    .name("카페")
                    .address("서울시 강남구")
                    .latitude(37.0)
                    .longitude(127.0)
                    .build()
            );
            when(placeUseCase.searchPlaces(anyString())).thenReturn(responses);

            // when & then
            mockMvc.perform(get(BASE + "/search").param("query", "카페"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("실패: query 파라미터가 없으면 400과 에러 코드를 반환한다")
        void fail_queryMissing() throws Exception {
            // when & then
            mockMvc.perform(get(BASE + "/search"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 서비스에서 예외가 발생하면 400과 에러 코드를 반환한다")
        void fail_serviceThrows() throws Exception {
            // given
            when(placeUseCase.searchPlaces(anyString()))
                .thenThrow(ApplicationException.from(CommonErrorCode.BAD_REQUEST));

            // when & then
            mockMvc.perform(get(BASE + "/search").param("query", "카페"))
                .andExpect(status().isBadRequest());
        }
    }
}