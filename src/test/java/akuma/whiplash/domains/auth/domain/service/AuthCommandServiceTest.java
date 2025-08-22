package akuma.whiplash.domains.auth.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import akuma.whiplash.global.config.security.jwt.JwtUtils;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.infrastructure.redis.RedisRepository;
import akuma.whiplash.infrastructure.redis.RedisService;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("AuthCommandService Unit Test")
@ExtendWith(MockitoExtension.class)
class AuthCommandServiceTest {

    @Mock
    private Map<String, Object> verifierMap;
    @Mock private MemberRepository memberRepository;
    @Mock private RedisRepository redisRepository;
    @Mock private JwtProvider jwtProvider;
    @Mock private JwtUtils jwtUtils;
    @Mock private RedisService redisService;

    @InjectMocks
    private AuthCommandServiceImpl authCommandService;

    private MemberContext context() {
        return MemberContext.builder()
            .memberId(1L)
            .deviceId("device")
            .build();
    }

    @Nested
    @DisplayName("logout - 로그아웃")
    class LogoutTest {

        @Test
        @DisplayName("성공: 리프레시 토큰과 FCM 토큰을 삭제한다")
        void success() {
            // given
            MemberContext ctx = context();

            // when
            authCommandService.logout(ctx);

            // then
            verify(jwtUtils).expireRefreshToken(1L, "device");
            verify(redisService).removeFcmTokenForDevice(1L, "device");
        }

        @Test
        @DisplayName("실패: 리프레시 토큰이 없으면 예외를 던진다")
        void fail_tokenNotFound() {
            // given
            MemberContext ctx = context();
            doThrow(ApplicationException.from(AuthErrorCode.INVALID_TOKEN))
                .when(jwtUtils).expireRefreshToken(1L, "device");

            // when & then
            assertThatThrownBy(() -> authCommandService.logout(ctx))
                .isInstanceOf(ApplicationException.class);
        }
    }
}