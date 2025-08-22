package akuma.whiplash.domains.place.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.config.IntegrationTest;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

// TODO: 테스트 코드 고치고 이 애노테이션 제거
@Disabled
@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("PlaceController Integration Test")
class PlaceControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private MemberRepository memberRepository;

    private static MockWebServer mockWebServer;

    @BeforeAll
    static void beforeAll() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void afterAll() throws Exception {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        // 서비스가 사용하는 base-url 속성 주입
        registry.add("naver.search.base-url",
            () -> mockWebServer.url("/").toString());
    }

    @Nested
    @DisplayName("[GET] /api/places/search - 장소 목록 검색")
    class SearchPlacesTest {

        @Test
        @DisplayName("성공: 검색어로 장소를 조회하면 200과 목록이 반환된다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            String token = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "device");
            String body = "{\"items\":[{\"title\":\"<b>카페</b>\",\"address\":\"서울시 강남구\",\"roadAddress\":\"서울시 강남구\",\"mapx\":\"127000000\",\"mapy\":\"37000000\"}]}";
            mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(body).addHeader("Content-Type", "application/json"));

            // when & then
            mockMvc.perform(get("/api/places/search")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .param("query", "카페"))
                .andExpect(status().isOk());
//                .andExpect(jsonPath("$.result[0].name").value("카페"));
        }

        @Test
        @DisplayName("실패: query 파라미터가 없으면 400과 에러 코드를 반환한다")
        void fail_queryMissing() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            String token = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "device");

            // when & then
            mockMvc.perform(get("/api/places/search")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 인증 토큰이 없으면 401과 에러 코드를 반환한다")
        void fail_tokenMissing() throws Exception {
            // when & then
            mockMvc.perform(get("/api/places/search").param("query", "카페"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 외부 API가 400을 반환하면 500과 에러 코드를 반환한다")
        void fail_externalApiError() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_3.toEntity());
            String token = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "device");
            mockWebServer.enqueue(new MockResponse().setResponseCode(400));

            // when & then
            mockMvc.perform(get("/api/places/search")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .param("query", "카페"))
                .andExpect(status().isInternalServerError());
        }
    }
}