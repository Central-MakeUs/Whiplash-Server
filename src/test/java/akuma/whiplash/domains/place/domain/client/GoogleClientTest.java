package akuma.whiplash.domains.place.domain.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import akuma.whiplash.domains.place.domain.client.impl.GoogleClientImpl;
import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
import akuma.whiplash.domains.place.domain.model.PlaceAutocompleteCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchCriteria;
import akuma.whiplash.domains.place.exception.PlaceErrorCode;
import akuma.whiplash.global.exception.ApplicationException;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

class GoogleClientTest {

    private MockWebServer mockWebServer;
    private GoogleClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create().responseTimeout(Duration.ofMillis(500))
            ))
            .build();
        client = new GoogleClientImpl(
            webClient,
            "test-key",
            mockWebServer.url("/v4/geocode/location").toString(),
            mockWebServer.url("/").toString()
        );
    }

    @Nested
    @DisplayName("autocomplete - Google 장소 자동완성")
    class AutocompleteTest {

        @Test
        @DisplayName("성공: Autocomplete 요청을 보내고 두 줄 추천 결과로 변환한다")
        void success() throws Exception {
            // given
            mockWebServer.enqueue(jsonResponse("""
                {
                  "suggestions":[{
                    "placePrediction":{
                      "placeId":"ChIJ",
                      "structuredFormat":{
                        "mainText":{"text":"구리시청"},
                        "secondaryText":{"text":"경기도 구리시 아차산로 439"}
                      }
                    }
                  }]
                }
                """));
            PlaceAutocompleteCriteria criteria = new PlaceAutocompleteCriteria(
                "구리", 37.5943, 127.1295, "ko", "KR",
                "550e8400-e29b-41d4-a716-446655440000"
            );

            // when
            var results = client.autocomplete(criteria);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).mainText()).isEqualTo("구리시청");
            assertThat(results.get(0).secondaryText()).isEqualTo("경기도 구리시 아차산로 439");
            assertThat(results.get(0).providerPlaceId()).isEqualTo("ChIJ");

            RecordedRequest request = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getMethod()).isEqualTo("POST");
            assertThat(request.getPath()).isEqualTo("/v1/places:autocomplete");
            assertThat(request.getHeader("X-Goog-Api-Key")).isEqualTo("test-key");
            assertThat(request.getHeader("X-Goog-FieldMask")).isEqualTo(
                "suggestions.placePrediction.placeId,"
                    + "suggestions.placePrediction.structuredFormat.mainText.text,"
                    + "suggestions.placePrediction.structuredFormat.secondaryText.text"
            );
            assertThat(request.getBody().readUtf8())
                .contains("\"input\":\"구리\"")
                .contains("\"languageCode\":\"ko\"")
                .contains("\"regionCode\":\"KR\"")
                .contains("\"sessionToken\":\"550e8400-e29b-41d4-a716-446655440000\"")
                .contains("\"locationBias\":{\"circle\":{\"center\":{\"latitude\":37.5943,\"longitude\":127.1295},\"radius\":5000.0}}");
        }

        @Test
        @DisplayName("성공: 보조 주소가 없어도 null로 변환한다")
        void success_withoutSecondaryText() throws Exception {
            // given
            mockWebServer.enqueue(jsonResponse("""
                {
                  "suggestions":[{
                    "placePrediction":{
                      "placeId":"ChIJ",
                      "structuredFormat":{"mainText":{"text":"구리시청"}}
                    }
                  }]
                }
                """));

            // when
            var results = client.autocomplete(new PlaceAutocompleteCriteria(
                "구리", null, null, null, null, "550e8400-e29b-41d4-a716-446655440000"
            ));

            // then
            assertThat(results.get(0).secondaryText()).isNull();
            RecordedRequest request = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getBody().readUtf8()).doesNotContain("locationBias");
        }

        @Test
        @DisplayName("실패: 추천 결과가 없으면 404 예외로 매핑한다")
        void fail_resultEmpty() {
            // given
            mockWebServer.enqueue(jsonResponse("{\"suggestions\":[]}"));

            // when & then
            assertPlaceError(
                () -> client.autocomplete(new PlaceAutocompleteCriteria(
                    "unknown", null, null, null, null,
                    "550e8400-e29b-41d4-a716-446655440000"
                )),
                PlaceErrorCode.AUTOCOMPLETE_NOT_FOUND
            );
        }

        @Test
        @DisplayName("실패: 인증되지 않은 요청이면 401 예외로 매핑한다")
        void fail_unauthorized() {
            // given
            mockWebServer.enqueue(new MockResponse().setResponseCode(401));

            // when & then
            assertAutocompleteError(PlaceErrorCode.PROVIDER_AUTHENTICATION_FAILED);
        }

        @Test
        @DisplayName("실패: 권한이 없는 요청이면 403 예외로 매핑한다")
        void fail_forbidden() {
            // given
            mockWebServer.enqueue(new MockResponse().setResponseCode(403));

            // when & then
            assertAutocompleteError(PlaceErrorCode.PROVIDER_PERMISSION_DENIED);
        }

        @Test
        @DisplayName("실패: 사용량 제한을 초과하면 409 예외로 매핑한다")
        void fail_quotaExceeded() {
            // given
            mockWebServer.enqueue(new MockResponse().setResponseCode(429));

            // when & then
            assertAutocompleteError(PlaceErrorCode.PROVIDER_QUOTA_EXCEEDED);
        }

        @Test
        @DisplayName("실패: provider 응답 시간이 초과되면 409 예외로 매핑한다")
        void fail_timeout() {
            // given
            mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

            // when & then
            assertAutocompleteError(PlaceErrorCode.PROVIDER_TIMEOUT);
        }

        @Test
        @DisplayName("실패: provider 응답 필수 필드가 없으면 409 예외로 매핑한다")
        void fail_malformedResponse() {
            // given
            mockWebServer.enqueue(jsonResponse("{\"suggestions\":[{\"placePrediction\":{}}]}"));

            // when & then
            assertAutocompleteError(PlaceErrorCode.PROVIDER_ERROR);
        }

        private void assertAutocompleteError(PlaceErrorCode errorCode) {
            assertPlaceError(
                () -> client.autocomplete(new PlaceAutocompleteCriteria(
                    "구리", null, null, null, null,
                    "550e8400-e29b-41d4-a716-446655440000"
                )),
                errorCode
            );
        }
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
            var results = client.searchPlaces(new PlaceSearchCriteria(
                "Starbucks", 40.7128, -74.0060, 3, "en", "US"
            ));

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
                .contains("\"regionCode\":\"US\"")
                .contains("\"locationBias\":{\"circle\":{\"center\":{\"latitude\":40.7128,\"longitude\":-74.006},\"radius\":5000.0}}");
        }

        @Test
        @DisplayName("성공: 현재 좌표가 없으면 location bias를 전송하지 않는다")
        void success_withoutCoordinates() throws Exception {
            // given
            mockWebServer.enqueue(jsonResponse("""
                {
                  "places":[{
                    "id":"ChIJ",
                    "displayName":{"text":"Starbucks","languageCode":"en"},
                    "formattedAddress":"New York, NY, USA",
                    "location":{"latitude":40.7128,"longitude":-74.0060}
                  }]
                }
                """));

            // when
            client.searchPlaces(new PlaceSearchCriteria("Starbucks", null, null, 5, null, null));

            // then
            RecordedRequest request = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getBody().readUtf8()).doesNotContain("locationBias");
        }

        @Test
        @DisplayName("실패: 검색 결과가 없으면 404 예외로 매핑한다")
        void fail_resultEmpty() {
            // given
            mockWebServer.enqueue(jsonResponse("{\"places\":[]}"));

            // when & then
            assertPlaceError(
                () -> client.searchPlaces(new PlaceSearchCriteria("unknown", null, null, 5, null, null)),
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
                  "results":[{
                    "formattedAddress":"경기도 구리시 갈매동",
                    "addressComponents":[
                      {"longText":"갈매동","shortText":"갈매동","types":["sublocality_level_2","political"]},
                      {"longText":"대한민국","shortText":"KR","types":["country","political"]}
                    ]
                  }]
                }
                """));

            // when
            var result = client.reverseGeocode(37.6340, 127.1150, "ko");

            // then
            assertThat(result.address()).isEqualTo("경기도 구리시 갈매동");
            assertThat(result.roadAddress()).isEqualTo("경기도 구리시 갈매동");
            assertThat(result.placeName()).isEqualTo("갈매동");
            assertThat(result.countryCode()).isEqualTo("KR");
            assertThat(result.latitude()).isEqualTo(37.6340);
            assertThat(result.longitude()).isEqualTo(127.1150);

            RecordedRequest request = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getMethod()).isEqualTo("GET");
            assertThat(request.getRequestUrl().encodedPath()).isEqualTo("/v4/geocode/location");
            assertThat(request.getRequestUrl().queryParameter("location.latitude")).isEqualTo("37.634");
            assertThat(request.getRequestUrl().queryParameter("location.longitude")).isEqualTo("127.115");
            assertThat(request.getRequestUrl().queryParameter("languageCode")).isEqualTo("ko");
            assertThat(request.getRequestUrl().queryParameter("key")).isNull();
            assertThat(request.getHeader("X-Goog-Api-Key")).isEqualTo("test-key");
            assertThat(request.getHeader("X-Goog-FieldMask")).isEqualTo(
                "results.formattedAddress,results.addressComponents,results.types"
            );
        }

        @Test
        @DisplayName("성공: 응답 언어가 없으면 language 파라미터를 전송하지 않는다")
        void success_withoutLanguage() throws Exception {
            // given
            mockWebServer.enqueue(jsonResponse("""
                {
                  "results":[{
                    "formattedAddress":"Sydney NSW, Australia",
                    "addressComponents":[
                      {"longText":"Sydney","shortText":"Sydney","types":["locality","political"]},
                      {"longText":"Australia","shortText":"AU","types":["country","political"]}
                    ]
                  }]
                }
                """));

            // when
            client.reverseGeocode(-33.8688, 151.2093, null);

            // then
            RecordedRequest request = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getRequestUrl().queryParameter("languageCode")).isNull();
        }

        @Test
        @DisplayName("실패: 결과가 비어 있으면 404 예외로 매핑한다")
        void fail_zeroResults() {
            // given
            mockWebServer.enqueue(jsonResponse("{\"results\":[]}"));

            // when & then
            assertPlaceError(() -> client.reverseGeocode(0.0, 0.0, null), PlaceErrorCode.PLACE_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 인증되지 않은 요청이면 401 예외로 매핑한다")
        void fail_unauthorized() {
            // given
            mockWebServer.enqueue(new MockResponse().setResponseCode(401));

            // when & then
            assertPlaceError(
                () -> client.reverseGeocode(40.0, -74.0, null),
                PlaceErrorCode.PROVIDER_AUTHENTICATION_FAILED
            );
        }

        @Test
        @DisplayName("실패: 권한이 없는 요청이면 403 예외로 매핑한다")
        void fail_forbidden() {
            // given
            mockWebServer.enqueue(new MockResponse().setResponseCode(403));

            // when & then
            assertPlaceError(
                () -> client.reverseGeocode(40.0, -74.0, null),
                PlaceErrorCode.PROVIDER_PERMISSION_DENIED
            );
        }

        @Test
        @DisplayName("실패: 사용량 제한을 초과하면 409 예외로 매핑한다")
        void fail_quotaExceeded() {
            // given
            mockWebServer.enqueue(new MockResponse().setResponseCode(429));

            // when & then
            assertPlaceError(
                () -> client.reverseGeocode(40.0, -74.0, null),
                PlaceErrorCode.PROVIDER_QUOTA_EXCEEDED
            );
        }

        @Test
        @DisplayName("실패: provider 응답 시간이 초과되면 409 예외로 매핑한다")
        void fail_timeout() {
            // given
            mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

            // when & then
            assertPlaceError(
                () -> client.reverseGeocode(40.0, -74.0, null),
                PlaceErrorCode.PROVIDER_TIMEOUT
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
