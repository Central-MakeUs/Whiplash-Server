package akuma.whiplash.infrastructure.firebase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import akuma.whiplash.infrastructure.redis.RedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("FcmService Unit Test")
@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @InjectMocks
    private FcmService fcmService;
    @Mock
    private RedisService redisService;

    @Nested
    @DisplayName("registerFcmToken - FCM 토큰 등록")
    class RegisterFcmTokenTest {

        @Test
        @DisplayName("성공: Redis에 토큰을 등록한다")
        void success() {
            // given
            Long memberId = 1L;
            String deviceId = "device1";
            String token = "token1";

            // when
            fcmService.registerFcmToken(memberId, deviceId, token);

            // then
            verify(redisService).upsertFcmToken(memberId, deviceId, token);
        }

        @Test
        @DisplayName("실패: Redis 오류 발생 시 예외를 전파한다")
        void fail_redisError() {
            // given
            Long memberId = 1L;
            String deviceId = "device2";
            String token = "token2";
            doThrow(new RuntimeException("redis error"))
                .when(redisService).upsertFcmToken(memberId, deviceId, token);

            // when & then
            assertThatThrownBy(() -> fcmService.registerFcmToken(memberId, deviceId, token))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("redis error");
        }
    }
}
