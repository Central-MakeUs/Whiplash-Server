package akuma.whiplash.domains.auth.application.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.domains.member.domain.contants.SocialType;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

@DisplayName("KakaoVerifier Unit Test")
class KakaoVerifierTest {

    private KakaoVerifier kakaoVerifier;
    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        kakaoVerifier = new KakaoVerifier(WebClient.builder().build());
        ReflectionTestUtils.setField(
            kakaoVerifier,
            "kakaoUserInfoUrl",
            mockWebServer.url("/v2/user/me").toString()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    private SocialLoginRequest buildSocialLoginRequest() {
        return new SocialLoginRequest(
            "KAKAO",
            "provider-access-token",
            "device-social-login",
            "ANDROID",
            "fcm-social-login",
            "1.0.0",
            "14"
        );
    }

    @Nested
    @DisplayName("verify - 카카오 사용자 정보 검증")
    class VerifyTest {

        @Test
        @DisplayName("성공: 이메일과 닉네임이 없어도 카카오 id로 회원 정보를 반환한다")
        void success_withoutEmailAndNickname() throws Exception {
            // given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                      "id": 123456789
                    }
                    """)
                .addHeader("Content-Type", "application/json"));

            // when
            SocialMemberInfo response = kakaoVerifier.verify(buildSocialLoginRequest());

            // then
            assertThat(response.provider()).isEqualTo(SocialType.KAKAO);
            assertThat(response.providerUserId()).isEqualTo("123456789");
            assertThat(response.email()).isNull();
            assertThat(response.name()).isNull();

            RecordedRequest request = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getPath()).isEqualTo("/v2/user/me");
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer provider-access-token");
        }

        @Test
        @DisplayName("성공: 카카오 계정 이메일과 프로필 닉네임을 반환한다")
        void success_withEmailAndNickname() {
            // given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                      "id": 123456789,
                      "properties": {
                        "nickname": "눈떠"
                      },
                      "kakao_account": {
                        "email": "nuntteo@test.com"
                      }
                    }
                    """)
                .addHeader("Content-Type", "application/json"));

            // when
            SocialMemberInfo response = kakaoVerifier.verify(buildSocialLoginRequest());

            // then
            assertThat(response.provider()).isEqualTo(SocialType.KAKAO);
            assertThat(response.providerUserId()).isEqualTo("123456789");
            assertThat(response.email()).isEqualTo("nuntteo@test.com");
            assertThat(response.name()).isEqualTo("눈떠");
        }

        @Test
        @DisplayName("실패: 카카오 id가 없으면 인증 실패 예외를 던진다")
        void fail_missingKakaoId() {
            // given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                      "kakao_account": {
                        "email": "nuntteo@test.com"
                      }
                    }
                    """)
                .addHeader("Content-Type", "application/json"));

            // when & then
            assertThatThrownBy(() -> kakaoVerifier.verify(buildSocialLoginRequest()))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(AuthErrorCode.AUTHENTICATION_FAILED)
                );
        }
    }
}
