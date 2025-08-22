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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

// TODO: 테스트 코드 고치고 이 애노테이션 제거
@Disabled
class PlaceQueryServiceTest {

    private PlaceQueryServiceImpl placeQueryService;
    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // ✅ MockWebServer 주소를 baseUrl로 설정하고, 네이버 헤더를 기본 헤더로 세팅
        WebClient webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString()) // e.g. http://127.0.0.1:50543/
            .defaultHeader("X-Naver-Client-Id", "test-id")
            .defaultHeader("X-Naver-Client-Secret", "test-secret")
            .build();

        // ✅ 구현체가 WebClient를 주입받아 상대경로로 호출한다고 가정
        placeQueryService = new PlaceQueryServiceImpl(webClient);
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
            // given (서비스의 변환 규칙에 맞춘 응답)
            String body = """
                {"items":[
                  {"title":"<b>카페</b>","roadAddress":"서울시 강남구","latitude":37.0,"longitude":127.0}
                ]}
                """;
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

            // when
            List<PlaceInfoResponse> responses = placeQueryService.searchPlaces("카페");

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).name()).isEqualTo("카페");           // <b> 제거 로직 반영
            assertThat(responses.get(0).address()).isEqualTo("서울시 강남구");
            assertThat(responses.get(0).latitude()).isEqualTo(37.0);
            assertThat(responses.get(0).longitude()).isEqualTo(127.0);

            // ✅ 실제로 MockWebServer가 호출되었는지 검증(외부로 나가지 않음 보장)
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

            // when & then (구현이 retrieve() 기본 onStatus 사용 시 WebClientResponseException 발생)
            assertThatThrownBy(() -> placeQueryService.searchPlaces("카페"))
                .isInstanceOf(WebClientResponseException.class);

            // ✅ 요청이 MockWebServer로 갔는지 확인
            RecordedRequest req = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
            assertThat(req).isNotNull();
            assertThat(req.getPath()).startsWith("/v1/search/local.json");
        }
    }
}