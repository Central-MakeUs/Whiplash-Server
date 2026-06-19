package akuma.whiplash.domains.place.presentation;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.config.IntegrationTest;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("PlaceController Integration Test")
class PlaceControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private MemberRepository memberRepository;

    @Nested
    @DisplayName("[GET] /api/v1/places/search - 장소 목록 검색")
    class SearchPlacesTest {

        @Test
        @DisplayName("성공: 검색어로 장소를 조회하면 200과 목록이 반환된다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            String token = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "device");

            // when
            var resultActions = mockMvc.perform(get("/api/v1/places/search")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .param("query", "카페"));

            // then
            resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].name").value("Mock Place 1 for 카페"))
                .andExpect(jsonPath("$.result[0].address").value("Seoul, Gangnam-gu, Teheran-ro 1"))
                .andExpect(jsonPath("$.result[0].distanceMeters").value(nullValue()));
        }

        @Test
        @DisplayName("실패: query 파라미터가 없으면 400과 에러 코드를 반환한다")
        void fail_queryMissing() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            String token = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "device");

            // when
            var resultActions = mockMvc.perform(get("/api/v1/places/search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

            // then
            resultActions.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 인증 토큰이 없으면 401과 에러 코드를 반환한다")
        void fail_tokenMissing() throws Exception {
            // when
            var resultActions = mockMvc.perform(get("/api/v1/places/search").param("query", "카페"));

            // then
            resultActions.andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("[GET] /api/v1/places/detail - 장소 상세 조회")
    class GetPlaceDetailTest {

        @Test
        @DisplayName("성공: 좌표로 장소 상세를 조회하면 200과 상세 정보가 반환된다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            String token = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "device");

            // when
            var resultActions = mockMvc.perform(get("/api/v1/places/detail")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .param("latitude", "37.4979")
                .param("longitude", "127.0276"));

            // then
            resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.placeName").value("Mock Detail Place"))
                .andExpect(jsonPath("$.result.address").value("Seoul, Gangnam-gu, Mock-ro 123"))
                .andExpect(jsonPath("$.result.roadAddress").value("Seoul, Gangnam-gu, Mock-ro 123"))
                .andExpect(jsonPath("$.result.latitude").value(37.4979))
                .andExpect(jsonPath("$.result.longitude").value(127.0276))
                .andExpect(jsonPath("$.result.countryCode").value("KR"))
                .andExpect(jsonPath("$.result.provider").value("NAVER"));
        }

        @Test
        @DisplayName("성공: 해외 좌표는 Google mock으로 장소 상세를 조회한다")
        void success_globalCoordinate() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            String token = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "device");

            // when
            var resultActions = mockMvc.perform(get("/api/v1/places/detail")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .param("latitude", "40.7128")
                .param("longitude", "-74.0060")
                .param("languageCode", "en"));

            // then
            resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.placeName").value("Mock Google Place"))
                .andExpect(jsonPath("$.result.address").value("Mock Google Address"))
                .andExpect(jsonPath("$.result.countryCode").value("US"))
                .andExpect(jsonPath("$.result.provider").value("GOOGLE"));
        }

        @Test
        @DisplayName("실패: 좌표 파라미터가 없으면 400과 에러 코드를 반환한다")
        void fail_coordinateMissing() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            String token = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "device");

            // when
            var resultActions = mockMvc.perform(get("/api/v1/places/detail")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .param("latitude", "37.4979"));

            // then
            resultActions.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 인증 토큰이 없으면 401과 에러 코드를 반환한다")
        void fail_tokenMissing() throws Exception {
            // when
            var resultActions = mockMvc.perform(get("/api/v1/places/detail")
                .param("latitude", "37.4979")
                .param("longitude", "127.0276"));

            // then
            resultActions.andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("[GET] /api/v1/places/keywords - 연관 장소 키워드 추천")
    class SearchPlaceKeywordsTest {

        @Test
        @DisplayName("성공: 검색어로 연관 키워드를 조회하면 200과 목록이 반환된다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_3.toEntity());
            String token = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "device");

            // when
            var resultActions = mockMvc.perform(get("/api/v1/places/keywords")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .param("query", "카페"));

            // then
            resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0]").value("카페동"))
                .andExpect(jsonPath("$.result[1]").value("카페로"))
                .andExpect(jsonPath("$.result[2]").value("카페길"));
        }

        @Test
        @DisplayName("실패: query 파라미터가 없으면 400과 에러 코드를 반환한다")
        void fail_queryMissing() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_4.toEntity());
            String token = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "device");

            // when
            var resultActions = mockMvc.perform(get("/api/v1/places/keywords")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

            // then
            resultActions.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 인증 토큰이 없으면 401과 에러 코드를 반환한다")
        void fail_tokenMissing() throws Exception {
            // when
            var resultActions = mockMvc.perform(get("/api/v1/places/keywords")
                .param("query", "카페"));

            // then
            resultActions.andExpect(status().isUnauthorized());
        }
    }
}
