package akuma.whiplash.domains.place.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import akuma.whiplash.domains.place.application.dto.response.PlaceInfoResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

class PlaceQueryServiceTest {

    private PlaceQueryServiceImpl placeQueryService;
    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder().build();

        placeQueryService = new PlaceQueryServiceImpl(webClient);
        ReflectionTestUtils.setField(placeQueryService, "naverClientId", "test-id");
        ReflectionTestUtils.setField(placeQueryService, "naverClientSecret", "test-secret");
        ReflectionTestUtils.setField(
            placeQueryService,
            "naverLocalSearchUrl",
            mockWebServer.url("/v1/search/local.json").toString()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("searchPlaces - 장소 검색")
    class SearchPlacesTest {

        @Test
        @DisplayName("성공: 키워드 검색 결과를 반환한다")
        void success() throws Exception {
            // given
            String body = """
                {"items":[
                  {"title":"<b>카페</b>","address":"서울시 강남구 역삼동","roadAddress":"서울시 강남구","mapx":"1270000000","mapy":"370000000"}
                ]}
                """;
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

            // when
            List<PlaceInfoResponse> responses = placeQueryService.searchPlaces("카페", null, null);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).name()).isEqualTo("카페");           // <b> 제거 로직 반영
            assertThat(responses.get(0).address()).isEqualTo("서울시 강남구");
            assertThat(responses.get(0).latitude()).isEqualTo(37.0);
            assertThat(responses.get(0).longitude()).isEqualTo(127.0);

            RecordedRequest req = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
            assertThat(req).isNotNull();
            assertThat(req.getPath()).startsWith("/v1/search/local.json");
            assertThat(req.getHeader("X-Naver-Client-Id")).isEqualTo("test-id");
            assertThat(req.getHeader("X-Naver-Client-Secret")).isEqualTo("test-secret");
        }

        @Test
        @DisplayName("실패: 외부 API가 에러를 반환하면 예외를 던진다")
        void fail_externalApiError() throws Exception {
            // given
            mockWebServer.enqueue(new MockResponse().setResponseCode(400));

            // when & then
            assertThatThrownBy(() -> placeQueryService.searchPlaces("카페", null, null))
                .isInstanceOf(WebClientResponseException.class);

            RecordedRequest req = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
            assertThat(req).isNotNull();
            assertThat(req.getPath()).startsWith("/v1/search/local.json");
        }
    }
}
