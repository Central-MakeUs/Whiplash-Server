package akuma.whiplash.domains.place.domain.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import akuma.whiplash.domains.place.domain.client.impl.NaverClientImpl;
import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

class NaverClientTest {

    private MockWebServer mockWebServer;
    private NaverClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        client = new NaverClientImpl(
            WebClient.builder().build(),
            "test-map-id",
            "test-map-secret",
            mockWebServer.url("/gc").toString(),
            "test-id",
            "test-secret",
            mockWebServer.url("/v1/search/local.json").toString()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("reverseGeocode - Naver reverse geocoding")
    class ReverseGeocodeTest {

        @Test
        @DisplayName("성공: 도로명 주소를 기존 장소 상세 계약으로 변환한다")
        void success() throws Exception {
            // given
            mockWebServer.enqueue(jsonResponse("""
                {
                  "status":{"code":0,"name":"ok","message":"done"},
                  "results":[{
                    "name":"roadaddr",
                    "region":{
                      "area0":{"name":"kr"},
                      "area1":{"name":"서울특별시"},
                      "area2":{"name":"강남구"},
                      "area3":{"name":"역삼동"},
                      "area4":{"name":""}
                    },
                    "land":{
                      "name":"강남대로",
                      "number1":"396",
                      "number2":"",
                      "addition0":{"type":"building","value":"강남역"}
                    }
                  }]
                }
                """));

            // when
            var result = client.reverseGeocode(37.4979, 127.0276);

            // then
            assertThat(result.address()).isEqualTo("서울특별시 강남구 역삼동 강남대로 396");
            assertThat(result.roadAddress()).isEqualTo(result.address());
            assertThat(result.placeName()).isEqualTo("강남역");
            assertThat(result.countryCode()).isEqualTo("KR");
            assertThat(result.provider()).isEqualTo(PlaceProvider.NAVER);
            assertThat(result.latitude()).isEqualTo(37.4979);
            assertThat(result.longitude()).isEqualTo(127.0276);

            RecordedRequest request = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getRequestUrl().queryParameter("coords")).isEqualTo("127.0276,37.4979");
            assertThat(request.getHeader("x-ncp-apigw-api-key-id")).isEqualTo("test-map-id");
            assertThat(request.getHeader("x-ncp-apigw-api-key")).isEqualTo("test-map-secret");
        }

        @Test
        @DisplayName("실패: 결과 없음 상태이면 404 예외로 매핑한다")
        void fail_noResults() {
            // given
            mockWebServer.enqueue(jsonResponse("""
                {"status":{"code":3,"name":"no results","message":"no results"},"results":[]}
                """));

            // when & then
            assertPlaceError(() -> client.reverseGeocode(37.0, 127.0), PlaceErrorCode.PLACE_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 인증 실패 응답이면 401 예외로 매핑한다")
        void fail_authentication() {
            // given
            mockWebServer.enqueue(new MockResponse().setResponseCode(401));

            // when & then
            assertPlaceError(
                () -> client.reverseGeocode(37.0, 127.0),
                PlaceErrorCode.PROVIDER_AUTHENTICATION_FAILED
            );
        }

        @Test
        @DisplayName("실패: 잘못된 provider 요청 상태이면 409 예외로 매핑한다")
        void fail_invalidProviderRequest() {
            // given
            mockWebServer.enqueue(jsonResponse("""
                {"status":{"code":100,"name":"invalid request","message":"invalid"},"results":[]}
                """));

            // when & then
            assertPlaceError(() -> client.reverseGeocode(37.0, 127.0), PlaceErrorCode.PROVIDER_ERROR);
        }

        @Test
        @DisplayName("실패: provider 응답 시간이 초과되면 409 timeout 예외로 매핑한다")
        void fail_timeout() {
            // given
            HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofMillis(100));
            WebClient timeoutWebClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
            client = new NaverClientImpl(
                timeoutWebClient,
                "test-map-id",
                "test-map-secret",
                mockWebServer.url("/gc").toString(),
                "test-id",
                "test-secret",
                mockWebServer.url("/v1/search/local.json").toString()
            );
            mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

            // when & then
            assertPlaceError(() -> client.reverseGeocode(37.0, 127.0), PlaceErrorCode.PROVIDER_TIMEOUT);
        }
    }

    @Nested
    @DisplayName("searchLocal - Naver 장소 검색")
    class SearchLocalTest {

        @Test
        @DisplayName("성공: 검색 결과와 요청 인증 정보를 반환한다")
        void success() throws Exception {
            // given
            mockWebServer.enqueue(jsonResponse("""
                {"items":[
                  {"title":"<b>카페</b>","address":"서울시 강남구 역삼동","roadAddress":"서울시 강남구","mapx":"1270000000","mapy":"370000000"}
                ]}
                """));

            // when
            var response = client.searchLocal("카페");

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).title()).isEqualTo("<b>카페</b>");
            RecordedRequest request = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getRequestUrl().queryParameter("query")).isEqualTo("카페");
            assertThat(request.getRequestUrl().queryParameter("display")).isEqualTo("5");
            assertThat(request.getHeader("X-Naver-Client-Id")).isEqualTo("test-id");
            assertThat(request.getHeader("X-Naver-Client-Secret")).isEqualTo("test-secret");
        }

        @Test
        @DisplayName("성공: 요청 size를 display에 반영하고 표준 검색 결과로 변환한다")
        void success_placeSearch() throws Exception {
            // given
            mockWebServer.enqueue(jsonResponse("""
                {"items":[
                  {"title":"<b>카페</b>","address":"서울시 강남구 역삼동","roadAddress":"서울시 강남구","mapx":"1270000000","mapy":"370000000"}
                ]}
                """));

            // when
            var results = client.searchPlaces("카페", 3);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).name()).isEqualTo("카페");
            assertThat(results.get(0).address()).isEqualTo("서울시 강남구");
            assertThat(results.get(0).latitude()).isEqualTo(37.0);
            assertThat(results.get(0).longitude()).isEqualTo(127.0);
            assertThat(results.get(0).provider()).isEqualTo(PlaceProvider.NAVER);
            assertThat(results.get(0).providerPlaceId()).isNull();
            assertThat(results.get(0).countryCode()).isEqualTo("KR");

            RecordedRequest request = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getRequestUrl().queryParameter("display")).isEqualTo("3");
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
