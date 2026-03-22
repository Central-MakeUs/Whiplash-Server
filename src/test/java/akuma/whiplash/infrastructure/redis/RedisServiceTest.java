package akuma.whiplash.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import akuma.whiplash.common.config.RedisContainerInitializer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

        @Test
        @DisplayName("성공: 동일 deviceId에 동시 요청이 와도 tokenCount가 1이다")
        void success_concurrentUpsertKeepsOneToken() throws InterruptedException {
            // given
            Long memberId = 1L;
            String deviceId = "shared-device";
            int threadCount = 10;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch ready = new CountDownLatch(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final String token = "token-" + i;
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        redisService.upsertFcmToken(memberId, deviceId, token);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            ready.await();
            start.countDown();
            done.await();
            executor.shutdown();

            // then
            assertThat(redisService.getFcmTokens(memberId)).hasSize(1);
        }
    }
}
