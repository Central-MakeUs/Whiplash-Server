package akuma.whiplash.domains.place.presentation;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.domains.place.application.dto.response.PlaceDetailResponse;
import akuma.whiplash.domains.place.application.dto.response.PlaceInfoResponse;
import akuma.whiplash.domains.place.application.usecase.PlaceUseCase;
import akuma.whiplash.domains.place.exception.PlaceErrorCode;
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

    private static final String BASE = "/api/v1/places";

    @Nested
    @DisplayName("searchPlaces - 장소 목록 검색")
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
                    .distanceMeters(120)
                    .build()
            );
            when(placeUseCase.searchPlaces(eq("카페"), eq(37.0), eq(127.0))).thenReturn(responses);

            // when
            var resultActions = mockMvc.perform(get(BASE + "/search")
                .param("query", "카페")
                .param("latitude", "37.0")
                .param("longitude", "127.0"));

            // then
            resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].name").value("카페"))
                .andExpect(jsonPath("$.result[0].address").value("서울시 강남구"))
                .andExpect(jsonPath("$.result[0].distanceMeters").value(120));
        }

        @Test
        @DisplayName("성공: 현재 좌표가 없으면 distanceMeters null을 반환한다")
        void success_withoutCoordinates() throws Exception {
            // given
            List<PlaceInfoResponse> responses = List.of(
                PlaceInfoResponse.builder()
                    .name("카페")
                    .address("서울시 강남구")
                    .latitude(37.0)
                    .longitude(127.0)
                    .distanceMeters(null)
                    .build()
            );
            when(placeUseCase.searchPlaces(eq("카페"), isNull(), isNull())).thenReturn(responses);

            // when
            var resultActions = mockMvc.perform(get(BASE + "/search").param("query", "카페"));

            // then
            resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].distanceMeters").value(nullValue()));
        }

        @Test
        @DisplayName("실패: query 파라미터가 없으면 400과 에러 코드를 반환한다")
        void fail_queryMissing() throws Exception {
            // when
            var resultActions = mockMvc.perform(get(BASE + "/search"));

            // then
            resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCustomCode()));
        }

        @Test
        @DisplayName("실패: query가 빈 문자열이면 400과 에러 코드를 반환한다")
        void fail_queryBlank() throws Exception {
            // given
            when(placeUseCase.searchPlaces(eq(""), isNull(), isNull()))
                .thenThrow(ApplicationException.from(CommonErrorCode.BAD_REQUEST));

            // when
            var resultActions = mockMvc.perform(get(BASE + "/search").param("query", ""));

            // then
            resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCustomCode()));
        }

        @Test
        @DisplayName("실패: 서비스에서 예외가 발생하면 400과 에러 코드를 반환한다")
        void fail_serviceThrows() throws Exception {
            // given
            when(placeUseCase.searchPlaces(anyString(), nullable(Double.class), nullable(Double.class)))
                .thenThrow(ApplicationException.from(CommonErrorCode.BAD_REQUEST));

            // when
            var resultActions = mockMvc.perform(get(BASE + "/search").param("query", "카페"));

            // then
            resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCustomCode()));
        }

        @Test
        @DisplayName("실패: 현재 좌표가 하나만 전달되면 400과 에러 코드를 반환한다")
        void fail_partialCoordinates() throws Exception {
            // given
            when(placeUseCase.searchPlaces(eq("카페"), eq(37.0), isNull()))
                .thenThrow(ApplicationException.from(CommonErrorCode.BAD_REQUEST));

            // when
            var resultActions = mockMvc.perform(get(BASE + "/search")
                .param("query", "카페")
                .param("latitude", "37.0"));

            // then
            resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCustomCode()));
        }
    }

    @Nested
    @DisplayName("getPlaceDetail - 장소 상세 조회")
    class GetPlaceDetailTest {

        @Test
        @DisplayName("성공: 200 OK와 장소 상세 정보를 반환한다")
        void success() throws Exception {
            // given
            PlaceDetailResponse response = PlaceDetailResponse.builder()
                .placeName("New York")
                .address("New York, NY, USA")
                .roadAddress("New York, NY, USA")
                .latitude(40.7128)
                .longitude(-74.0060)
                .countryCode("US")
                .provider("GOOGLE")
                .build();
            when(placeUseCase.getPlaceDetail(eq(40.7128), eq(-74.0060), eq("en"))).thenReturn(response);

            // when
            var resultActions = mockMvc.perform(get(BASE + "/detail")
                .param("latitude", "40.7128")
                .param("longitude", "-74.0060")
                .param("languageCode", "en"));

            // then
            resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.placeName").value("New York"))
                .andExpect(jsonPath("$.result.address").value("New York, NY, USA"))
                .andExpect(jsonPath("$.result.roadAddress").value("New York, NY, USA"))
                .andExpect(jsonPath("$.result.latitude").value(40.7128))
                .andExpect(jsonPath("$.result.longitude").value(-74.0060))
                .andExpect(jsonPath("$.result.countryCode").value("US"))
                .andExpect(jsonPath("$.result.provider").value("GOOGLE"));
        }

        @Test
        @DisplayName("실패: 좌표 파라미터가 없으면 400과 에러 코드를 반환한다")
        void fail_coordinateMissing() throws Exception {
            // when
            var resultActions = mockMvc.perform(get(BASE + "/detail")
                .param("latitude", "37.4979"));

            // then
            resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCustomCode()));
        }

        @Test
        @DisplayName("실패: 서비스에서 예외가 발생하면 400과 에러 코드를 반환한다")
        void fail_serviceThrows() throws Exception {
            // given
            when(placeUseCase.getPlaceDetail(eq(37.4979), eq(127.0276), isNull()))
                .thenThrow(ApplicationException.from(CommonErrorCode.BAD_REQUEST));

            // when
            var resultActions = mockMvc.perform(get(BASE + "/detail")
                .param("latitude", "37.4979")
                .param("longitude", "127.0276"));

            // then
            resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCustomCode()));
        }

        @Test
        @DisplayName("실패: 지원하지 않는 응답 언어이면 400과 에러 코드를 반환한다")
        void fail_unsupportedLanguage() throws Exception {
            // given
            when(placeUseCase.getPlaceDetail(eq(37.4979), eq(127.0276), eq("ja")))
                .thenThrow(ApplicationException.from(PlaceErrorCode.UNSUPPORTED_LANGUAGE));

            // when
            var resultActions = mockMvc.perform(get(BASE + "/detail")
                .param("latitude", "37.4979")
                .param("longitude", "127.0276")
                .param("languageCode", "ja"));

            // then
            resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(PlaceErrorCode.UNSUPPORTED_LANGUAGE.getCustomCode()));
        }
    }

    @Nested
    @DisplayName("searchPlaceKeywords - 연관 장소 키워드 추천")
    class SearchPlaceKeywordsTest {

        @Test
        @DisplayName("성공: 200 OK와 연관 키워드 목록을 반환한다")
        void success() throws Exception {
            // given
            List<String> responses = List.of("포켓몬", "포켓몬카드", "포켓몬 팝업");
            when(placeUseCase.searchPlaceKeywords(eq("포켓몬"))).thenReturn(responses);

            // when
            var resultActions = mockMvc.perform(get(BASE + "/keywords")
                .param("query", "포켓몬"));

            // then
            resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0]").value("포켓몬"))
                .andExpect(jsonPath("$.result[1]").value("포켓몬카드"))
                .andExpect(jsonPath("$.result[2]").value("포켓몬 팝업"));
        }

        @Test
        @DisplayName("실패: query 파라미터가 없으면 400과 에러 코드를 반환한다")
        void fail_queryMissing() throws Exception {
            // when
            var resultActions = mockMvc.perform(get(BASE + "/keywords"));

            // then
            resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCustomCode()));
        }

        @Test
        @DisplayName("실패: 서비스에서 예외가 발생하면 400과 에러 코드를 반환한다")
        void fail_serviceThrows() throws Exception {
            // given
            when(placeUseCase.searchPlaceKeywords(eq("포켓몬")))
                .thenThrow(ApplicationException.from(CommonErrorCode.BAD_REQUEST));

            // when
            var resultActions = mockMvc.perform(get(BASE + "/keywords")
                .param("query", "포켓몬"));

            // then
            resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCustomCode()));
        }
    }
}
