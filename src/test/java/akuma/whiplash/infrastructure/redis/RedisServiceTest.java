package akuma.whiplash.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import akuma.whiplash.common.config.RedisContainerInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DisplayName("RedisService Repository Test")
@DataRedisTest
@ActiveProfiles("test")
@Import({RedisService.class})
@ContextConfiguration(initializers = {RedisContainerInitializer.class})
class RedisServiceTest {

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @AfterEach
    void clean() {
        try (RedisConnection conn = connectionFactory.getConnection()) {
            conn.serverCommands().flushAll();
        }
    }

    @Nested
    @DisplayName("upsertFcmToken - FCM 토큰 등록/갱신")
    class UpsertFcmTokenTest {

        @Test
        @DisplayName("성공: 새로운 토큰을 저장한다")
        void success_saveNewToken() {
            Long memberId = 1L;
            String deviceId = "deviceA";
            String token = "tokenA";

            redisService.upsertFcmToken(memberId, deviceId, token);

            assertThat(redisService.getFcmTokens(memberId))
                .containsExactlyInAnyOrder(token);
        }

        @Test
        @DisplayName("성공: 다른 토큰으로 교체하면 이전 토큰이 제거된다")
        void success_replaceToken() {
            Long memberId = 1L;
            String deviceId = "deviceB";
            redisService.upsertFcmToken(memberId, deviceId, "oldToken");

            redisService.upsertFcmToken(memberId, deviceId, "newToken");

            assertThat(redisService.getFcmTokens(memberId))
                .containsExactlyInAnyOrder("newToken");
            assertThat(redisService.getDeviceIdByFcmToken("newToken")).isEqualTo(deviceId);
            assertThat(redisService.getDeviceIdByFcmToken("oldToken")).isNull();
        }

        @Test
        @DisplayName("실패: deviceId가 null이면 예외를 던진다")
        void fail_deviceIdNull() {
            assertThatThrownBy(() -> redisService.upsertFcmToken(1L, null, "token"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("실패: 토큰이 null이면 예외를 던진다")
        void fail_tokenNull() {
            assertThatThrownBy(() -> redisService.upsertFcmToken(1L, "device", null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
