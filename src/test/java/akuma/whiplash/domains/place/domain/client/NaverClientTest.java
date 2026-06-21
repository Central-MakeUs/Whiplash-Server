package akuma.whiplash.domains.place.domain.client;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.domains.place.domain.client.impl.NaverClientImpl;
import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
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

class NaverClientTest {

    private MockWebServer mockWebServer;
    private NaverClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        client = new NaverClientImpl(
            WebClient.builder().build(),
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

}
