package akuma.whiplash.domains.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.config.IntegrationTest;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import akuma.whiplash.infrastructure.redis.RedisRepository;
import akuma.whiplash.infrastructure.redis.RedisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("AuthController Integration Test")
@IntegrationTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private MemberRepository memberRepository;
    @Autowired private RedisRepository redisRepository;
    @Autowired private RedisService redisService;
    @Autowired private ObjectMapper objectMapper;

    @Value("${jwt.secret-key}")
    private String secret;

    private static final String BASE = "/api/auth";
    private static final String DEVICE = "device";

    @Nested
    @DisplayName("[POST] /api/auth/logout - 로그아웃")
    class LogoutTest {

        @Test
        @DisplayName("성공: 리프레시 토큰과 FCM 토큰을 삭제한다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            String deviceId = "device-success";
            String refreshToken = jwtProvider.generateRefreshToken(member.getId(), deviceId, member.getRole());
            redisService.upsertFcmToken(member.getId(), deviceId, "fcmToken");

            // when
            mockMvc.perform(post("/api/auth/logout")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
                .andExpect(status().isOk());

            // then
            assertThat(redisRepository.getValues("REFRESH:" + member.getId() + ":" + deviceId)).isEmpty();
            assertThat(redisService.getFcmTokenByDevice(deviceId)).isNull();
        }

        @Test
        @DisplayName("실패: 유효하지 않은 토큰이면 401과 에러 코드를 반환한다")
        void fail_invalidToken() throws Exception {
            // given
            String invalidToken = "invalid.token.value";

            // when
            MvcResult result = mockMvc.perform(post("/api/auth/logout")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized())
                .andReturn();

            // then
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.get("code").asText()).isEqualTo("AUTH_102");
        }

        @Test
        @DisplayName("실패: 만료된 토큰이면 401과 에러 코드를 반환한다")
        void fail_tokenExpired() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            String deviceId = "device-expired";
            String secret = (String) ReflectionTestUtils.getField(jwtProvider, "secret");
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
            String expiredToken = Jwts.builder()
                .claim("role", member.getRole())
                .claim("type", "REFRESH")
                .claim("deviceId", deviceId)
                .setSubject(member.getId().toString())
                .setExpiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
            redisRepository.setValues("REFRESH:" + member.getId() + ":" + deviceId, expiredToken, Duration.ofMinutes(60));

            // when
            MvcResult result = mockMvc.perform(post("/api/auth/logout")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andReturn();

            // then
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.get("code").asText()).isEqualTo("AUTH_103");
        }

        @Test
        @DisplayName("실패: 토큰이 저장되어 있지 않으면 401와 에러 코드를 반환한다")
        void fail_tokenNotFound() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_3.toEntity());
            String deviceId = "device-notfound";
            String refreshToken = jwtProvider.generateRefreshToken(member.getId(), deviceId, member.getRole());
            redisRepository.deleteValues("REFRESH:" + member.getId() + ":" + deviceId);

            // when
            MvcResult result = mockMvc.perform(post("/api/auth/logout")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized())
                .andReturn();

            // then
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.get("code").asText()).isEqualTo("AUTH_102");
        }
    }

    @Nested
    @DisplayName("[POST] /api/auth/reissue - 토큰 재발급")
    class ReissueTokenTest {

/*        @Test
        @DisplayName("성공: 200 OK와 새로운 토큰을 반환한다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            String deviceId = "device-1";

            String refreshToken = jwtProvider.generateRefreshToken(member.getId(), deviceId, member.getRole());

            // when & then
            mockMvc.perform(post("/api/auth/reissue")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
                .andExpect(status().isOk());
        }*/

        @Test
        @DisplayName("실패: 토큰이 유효하지 않으면 401과 에러 코드를 반환한다")
        void fail_invalidToken() throws Exception {
            // when & then
            mockMvc.perform(post(BASE + "/reissue")
                    .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_TOKEN.getCustomCode()));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 리프레시 토큰이면 401와 에러 코드를 반환한다")
        void fail_tokenNotFound() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            String refreshToken = jwtProvider.generateRefreshToken(member.getId(), DEVICE, member.getRole());
            redisRepository.deleteValues("REFRESH:" + member.getId() + ":" + DEVICE);

            // when & then
            mockMvc.perform(post(BASE + "/reissue")
                    .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_TOKEN.getCustomCode()));
        }

        @Test
        @DisplayName("실패: 만료된 리프레시 토큰이면 401과 에러 코드를 반환한다")
        void fail_tokenExpired() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_3.toEntity());
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
            String expiredToken = Jwts.builder()
                .claim("role", member.getRole())
                .claim("type", "REFRESH")
                .claim("deviceId", DEVICE)
                .setSubject(member.getId().toString())
                .setExpiration(Date.from(Instant.now().minusSeconds(10)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
            redisRepository.setValues("REFRESH:" + member.getId() + ":" + DEVICE, expiredToken, Duration.ofMinutes(5));

            // when & then
            mockMvc.perform(post(BASE + "/reissue")
                    .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.TOKEN_EXPIRED.getCustomCode()));
        }
    }
}