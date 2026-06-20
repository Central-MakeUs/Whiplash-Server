package akuma.whiplash.domains.place.domain.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import akuma.whiplash.domains.place.domain.client.impl.GoogleClientImpl;
import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
import akuma.whiplash.domains.place.exception.PlaceErrorCode;
import akuma.whiplash.global.exception.ApplicationException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class GoogleClientTest {

    private MockWebServer mockWebServer;
    private GoogleClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        client = new GoogleClientImpl(
            WebClient.builder().build(),
            "test-key",
            mockWebServer.url("/geocode/json").toString(),
            mockWebServer.url("/").toString()
        );
    }

    @Nested
    @DisplayName("searchPlaces - Google 장소 검색")
    class SearchPlacesTest {

        @Test
        @DisplayName("성공: Text Search 요청을 보내고 표준 검색 결과로 변환한다")
        void success() throws Exception {
            // given
            mockWebServer.enqueue(jsonResponse("""
                {
                  "places":[{
                    "id":"ChIJ",
                    "displayName":{"text":"Starbucks","languageCode":"en"},
                    "formattedAddress":"New York, NY, USA",
                    "location":{"latitude":40.7128,"longitude":-74.0060},
                    "addressComponents":[
                      {"longText":"United States","shortText":"US","types":["country"]}
                    ]
                  }]
                }
                """));

            // when
            var results = client.searchPlaces("Starbucks", 3, "en", "US");

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).name()).isEqualTo("Starbucks");
            assertThat(results.get(0).address()).isEqualTo("New York, NY, USA");
            assertThat(results.get(0).provider()).isEqualTo(PlaceProvider.GOOGLE);
            assertThat(results.get(0).providerPlaceId()).isEqualTo("ChIJ");
            assertThat(results.get(0).countryCode()).isEqualTo("US");

            RecordedRequest request = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getMethod()).isEqualTo("POST");
            assertThat(request.getPath()).isEqualTo("/v1/places:searchText");
            assertThat(request.getHeader("X-Goog-Api-Key")).isEqualTo("test-key");
            assertThat(request.getHeader("X-Goog-FieldMask")).isEqualTo(
                "places.id,places.displayName,places.formattedAddress,places.location,places.addressComponents"
            );
            assertThat(request.getBody().readUtf8())
                .contains("\"textQuery\":\"Starbucks\"")
                .contains("\"pageSize\":3")
                .contains("\"languageCode\":\"en\"")
                .contains("\"regionCode\":\"US\"");
        }

        @Test
        @DisplayName("실패: 검색 결과가 없으면 404 예외로 매핑한다")
        void fail_resultEmpty() {
            // given
            mockWebServer.enqueue(jsonResponse("{\"places\":[]}"));

            // when & then
            assertPlaceError(
                () -> client.searchPlaces("unknown", 5, null, null),
                PlaceErrorCode.PLACE_NOT_FOUND
            );
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("reverseGeocode - Google reverse geocoding")
    class ReverseGeocodeTest {

        @Test
        @DisplayName("성공: 첫 결과를 표준 장소 상세 정보로 변환한다")
        void success() throws Exception {
            // given
            mockWebServer.enqueue(jsonResponse("""
                {
                  "status":"OK",
                  "results":[{
                    "formatted_address":"New York, NY, USA",
                    "address_components":[
                      {"long_name":"New York","short_name":"New York","types":["locality","political"]},
                      {"long_name":"United States","short_name":"US","types":["country","political"]}
                    ]
                  }]
                }
                """));

            // when
            var result = client.reverseGeocode(40.7128, -74.0060, "en");

            // then
            assertThat(result.address()).isEqualTo("New York, NY, USA");
            assertThat(result.roadAddress()).isEqualTo("New York, NY, USA");
            assertThat(result.placeName()).isEqualTo("New York");
            assertThat(result.countryCode()).isEqualTo("US");
            assertThat(result.provider()).isEqualTo(PlaceProvider.GOOGLE);
            assertThat(result.latitude()).isEqualTo(40.7128);
            assertThat(result.longitude()).isEqualTo(-74.0060);

            RecordedRequest request = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getRequestUrl().queryParameter("latlng")).isEqualTo("40.7128,-74.006");
            assertThat(request.getRequestUrl().queryParameter("language")).isEqualTo("en");
            assertThat(request.getRequestUrl().queryParameter("key")).isEqualTo("test-key");
        }

        @Test
        @DisplayName("성공: 응답 언어가 없으면 language 파라미터를 전송하지 않는다")
        void success_withoutLanguage() throws Exception {
            // given
            mockWebServer.enqueue(jsonResponse("""
                {
                  "status":"OK",
                  "results":[{
                    "formatted_address":"Sydney NSW, Australia",
                    "address_components":[
                      {"long_name":"Sydney","short_name":"Sydney","types":["locality","political"]},
                      {"long_name":"Australia","short_name":"AU","types":["country","political"]}
                    ]
                  }]
                }
                """));

            // when
            client.reverseGeocode(-33.8688, 151.2093, null);

            // then
            RecordedRequest request = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getRequestUrl().queryParameter("language")).isNull();
        }

        @Test
        @DisplayName("실패: 결과 없음 상태이면 404 예외로 매핑한다")
        void fail_zeroResults() {
            // given
            mockWebServer.enqueue(jsonResponse("""
                {"status":"ZERO_RESULTS","results":[]}
                """));

            // when & then
            assertPlaceError(() -> client.reverseGeocode(0.0, 0.0, null), PlaceErrorCode.PLACE_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 요청 거부 상태이면 403 예외로 매핑한다")
        void fail_requestDenied() {
            // given
            mockWebServer.enqueue(jsonResponse("""
                {"status":"REQUEST_DENIED","results":[]}
                """));

            // when & then
            assertPlaceError(
                () -> client.reverseGeocode(40.0, -74.0, null),
                PlaceErrorCode.PROVIDER_PERMISSION_DENIED
            );
        }

        @Test
        @DisplayName("실패: quota 초과 상태이면 409 예외로 매핑한다")
        void fail_quotaExceeded() {
            // given
            mockWebServer.enqueue(jsonResponse("""
                {"status":"OVER_QUERY_LIMIT","results":[]}
                """));

            // when & then
            assertPlaceError(
                () -> client.reverseGeocode(40.0, -74.0, null),
                PlaceErrorCode.PROVIDER_QUOTA_EXCEEDED
            );
        }

        @Test
        @DisplayName("실패: provider 응답을 해석할 수 없으면 409 예외로 매핑한다")
        void fail_malformedResponse() {
            // given
            mockWebServer.enqueue(jsonResponse("not-json"));

            // when & then
            assertPlaceError(
                () -> client.reverseGeocode(40.0, -74.0, null),
                PlaceErrorCode.PROVIDER_ERROR
            );
        }
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(body);
    }

    private void assertPlaceError(Runnable action, PlaceErrorCode errorCode) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(ApplicationException.class, e ->
                assertThat(e.getCode()).isEqualTo(errorCode)
            );
    }
}
