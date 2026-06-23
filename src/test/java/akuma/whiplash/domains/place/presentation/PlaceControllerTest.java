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
import akuma.whiplash.domains.place.application.dto.response.PlaceAutocompleteResponse;
import akuma.whiplash.domains.place.application.dto.response.PlaceAutocompleteSuggestionResponse;
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
    @DisplayName("getPlaceAutocompleteSuggestions - 장소 자동완성")
    class GetPlaceAutocompleteSuggestionsTest {

        private static final String SESSION_TOKEN = "550e8400-e29b-41d4-a716-446655440000";

        @Test
        @DisplayName("성공: provider 없이 두 줄 장소 추천 결과를 반환한다")
        void success() throws Exception {
            // given
            when(placeUseCase.getPlaceAutocompleteSuggestions(
                "구리", 37.5943, 127.1295, "ko", "KR", SESSION_TOKEN
            )).thenReturn(new PlaceAutocompleteResponse(List.of(
                new PlaceAutocompleteSuggestionResponse(
                    "구리시청", "경기도 구리시 아차산로 439", "ChIJ"
                )
            )));

            // when
            var resultActions = mockMvc.perform(get(BASE + "/autocomplete")
                .param("query", "구리")
                .param("latitude", "37.5943")
                .param("longitude", "127.1295")
                .param("languageCode", "ko")
                .param("regionCode", "KR")
                .param("sessionToken", SESSION_TOKEN));

            // then
            resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.suggestions[0].mainText").value("구리시청"))
                .andExpect(jsonPath("$.result.suggestions[0].secondaryText")
                    .value("경기도 구리시 아차산로 439"))
                .andExpect(jsonPath("$.result.suggestions[0].providerPlaceId").value("ChIJ"))
                .andExpect(jsonPath("$.result.suggestions[0].provider").doesNotExist());
        }

        @Test
        @DisplayName("실패: sessionToken 파라미터가 없으면 400을 반환한다")
        void fail_sessionTokenMissing() throws Exception {
            // when
            var resultActions = mockMvc.perform(get(BASE + "/autocomplete")
                .param("query", "구리"));

            // then
            resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCustomCode()));
        }

        @Test
        @DisplayName("실패: sessionToken이 UUID 형식이 아니면 400을 반환한다")
        void fail_sessionTokenInvalid() throws Exception {
            // given
            when(placeUseCase.getPlaceAutocompleteSuggestions(
                "구리", null, null, null, null, "invalid-token"
            )).thenThrow(ApplicationException.from(CommonErrorCode.BAD_REQUEST));

            // when
            var resultActions = mockMvc.perform(get(BASE + "/autocomplete")
                .param("query", "구리")
                .param("sessionToken", "invalid-token"));

            // then
            resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCustomCode()));
        }
    }

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
                    .provider("GOOGLE")
                    .providerPlaceId("ChIJ")
                    .countryCode("KR")
                    .build()
            );
            when(placeUseCase.searchPlaces(
                eq("카페"), eq(37.0), eq(127.0), eq(3), eq("ko"), eq("KR")
            )).thenReturn(responses);

            // when
            var resultActions = mockMvc.perform(get(BASE + "/search")
                .param("query", "카페")
                .param("latitude", "37.0")
                .param("longitude", "127.0")
                .param("size", "3")
                .param("languageCode", "ko")
                .param("regionCode", "KR"));

            // then
            resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].name").value("카페"))
                .andExpect(jsonPath("$.result[0].address").value("서울시 강남구"))
                .andExpect(jsonPath("$.result[0].distanceMeters").value(120))
                .andExpect(jsonPath("$.result[0].provider").value("GOOGLE"))
                .andExpect(jsonPath("$.result[0].providerPlaceId").value("ChIJ"))
                .andExpect(jsonPath("$.result[0].countryCode").value("KR"));
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
                    .provider("GOOGLE")
                    .providerPlaceId("ChIJ")
                    .countryCode("KR")
                    .build()
            );
            when(placeUseCase.searchPlaces(
                eq("카페"), isNull(), isNull(), eq(5), isNull(), isNull()
            )).thenReturn(responses);

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
            when(placeUseCase.searchPlaces(eq(""), isNull(), isNull(), eq(5), isNull(), isNull()))
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
            when(placeUseCase.searchPlaces(
                anyString(), nullable(Double.class), nullable(Double.class), eq(5), isNull(), isNull()
            ))
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
            when(placeUseCase.searchPlaces(eq("카페"), eq(37.0), isNull(), eq(5), isNull(), isNull()))
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

        @Test
        @DisplayName("실패: size가 최대값을 초과하면 400과 에러 코드를 반환한다")
        void fail_sizeOutOfRange() throws Exception {
            // given
            when(placeUseCase.searchPlaces("카페", null, null, 6, null, null))
                .thenThrow(ApplicationException.from(CommonErrorCode.BAD_REQUEST));

            // when
            var resultActions = mockMvc.perform(get(BASE + "/search")
                .param("query", "카페")
                .param("size", "6"));

            // then
            resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCustomCode()));
        }

        @Test
        @DisplayName("실패: 지원하지 않는 지역 코드이면 400과 에러 코드를 반환한다")
        void fail_unsupportedRegion() throws Exception {
            // given
            when(placeUseCase.searchPlaces("카페", null, null, 5, null, "JP"))
                .thenThrow(ApplicationException.from(PlaceErrorCode.UNSUPPORTED_REGION));

            // when
            var resultActions = mockMvc.perform(get(BASE + "/search")
                .param("query", "카페")
                .param("regionCode", "JP"));

            // then
            resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(PlaceErrorCode.UNSUPPORTED_REGION.getCustomCode()));
        }
    }

    @Nested
    @DisplayName("getPlaceReverseGeocode - 장소 역지오코딩")
    class GetPlaceReverseGeocodeTest {

        @Test
        @DisplayName("성공: 200 OK와 좌표 기반 주소를 반환한다")
        void success() throws Exception {
            // given
            PlaceDetailResponse response = PlaceDetailResponse.builder()
                .placeName("New York")
                .address("New York, NY, USA")
                .roadAddress("New York, NY, USA")
                .latitude(40.7128)
                .longitude(-74.0060)
                .countryCode("US")
                .build();
            when(placeUseCase.getPlaceReverseGeocode(
                eq(40.7128), eq(-74.0060), eq("en")
            )).thenReturn(response);

            // when
            var resultActions = mockMvc.perform(get(BASE + "/reverse-geocode")
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
                .andExpect(jsonPath("$.result.providerPlaceId").doesNotExist())
                .andExpect(jsonPath("$.result.provider").doesNotExist());
        }

        @Test
        @DisplayName("실패: 좌표 파라미터가 없으면 400과 에러 코드를 반환한다")
        void fail_coordinateMissing() throws Exception {
            // given
            // when
            var resultActions = mockMvc.perform(get(BASE + "/reverse-geocode")
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
            when(placeUseCase.getPlaceReverseGeocode(eq(37.4979), eq(127.0276), isNull()))
                .thenThrow(ApplicationException.from(CommonErrorCode.BAD_REQUEST));

            // when
            var resultActions = mockMvc.perform(get(BASE + "/reverse-geocode")
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
            when(placeUseCase.getPlaceReverseGeocode(eq(37.4979), eq(127.0276), eq("ja")))
                .thenThrow(ApplicationException.from(PlaceErrorCode.UNSUPPORTED_LANGUAGE));

            // when
            var resultActions = mockMvc.perform(get(BASE + "/reverse-geocode")
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
    @DisplayName("getPlaceDetails - 선택 장소 상세 조회")
    class GetPlaceDetailsTest {

        @Test
        @DisplayName("성공: 선택 장소는 장소명과 providerPlaceId 없이 반환한다")
        void success() throws Exception {
            // given
            PlaceDetailResponse response = PlaceDetailResponse.builder()
                .address("경기도 구리시 아차산로 439")
                .roadAddress("경기도 구리시 아차산로 439")
                .latitude(37.5943)
                .longitude(127.1296)
                .countryCode("KR")
                .build();
            when(placeUseCase.getPlaceDetails(
                eq("ChIJ"), eq("550e8400-e29b-41d4-a716-446655440000"), eq("ko"), eq("KR")
            )).thenReturn(response);

            // when
            var resultActions = mockMvc.perform(get(BASE + "/details")
                .param("providerPlaceId", "ChIJ")
                .param("sessionToken", "550e8400-e29b-41d4-a716-446655440000")
                .param("languageCode", "ko")
                .param("regionCode", "KR"));

            // then
            resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.address").value("경기도 구리시 아차산로 439"))
                .andExpect(jsonPath("$.result.placeName").value(nullValue()))
                .andExpect(jsonPath("$.result.roadAddress").value("경기도 구리시 아차산로 439"))
                .andExpect(jsonPath("$.result.latitude").value(37.5943))
                .andExpect(jsonPath("$.result.longitude").value(127.1296))
                .andExpect(jsonPath("$.result.countryCode").value("KR"))
                .andExpect(jsonPath("$.result.providerPlaceId").doesNotExist())
                .andExpect(jsonPath("$.result.provider").doesNotExist());
        }

        @Test
        @DisplayName("실패: sessionToken이 UUID 형식이 아니면 400을 반환한다")
        void fail_sessionTokenInvalid() throws Exception {
            // given
            when(placeUseCase.getPlaceDetails(eq("ChIJ"), eq("invalid-token"), isNull(), isNull()))
                .thenThrow(ApplicationException.from(CommonErrorCode.BAD_REQUEST));

            // when
            var resultActions = mockMvc.perform(get(BASE + "/details")
                .param("providerPlaceId", "ChIJ")
                .param("sessionToken", "invalid-token"));

            // then
            resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCustomCode()));
        }

        @Test
        @DisplayName("실패: 지원하지 않는 지역이면 400과 에러 코드를 반환한다")
        void fail_unsupportedRegion() throws Exception {
            // given
            when(placeUseCase.getPlaceDetails(
                eq("ChIJ"), eq("550e8400-e29b-41d4-a716-446655440000"), isNull(), eq("JP")
            )).thenThrow(ApplicationException.from(PlaceErrorCode.UNSUPPORTED_REGION));

            // when
            var resultActions = mockMvc.perform(get(BASE + "/details")
                .param("providerPlaceId", "ChIJ")
                .param("sessionToken", "550e8400-e29b-41d4-a716-446655440000")
                .param("regionCode", "JP"));

            // then
            resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(PlaceErrorCode.UNSUPPORTED_REGION.getCustomCode()));
        }

        @Test
        @DisplayName("실패: 기존 상세 조회 URI는 404를 반환한다")
        void fail_legacyDetailUri() throws Exception {
            // when
            var resultActions = mockMvc.perform(get(BASE + "/detail")
                .param("latitude", "37.4979")
                .param("longitude", "127.0276"));

            // then
            resultActions.andExpect(status().isNotFound());
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
