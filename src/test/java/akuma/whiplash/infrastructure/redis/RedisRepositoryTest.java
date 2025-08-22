package akuma.whiplash.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.common.config.RedisContainerInitializer;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@DisplayName("RedisRepository Slice Test")
@DataRedisTest
@Import(RedisRepositoryImpl.class)
@ContextConfiguration(initializers = RedisContainerInitializer.class)
class RedisRepositoryTest {

    @Autowired
    private RedisRepository redisRepository;

    @Nested
    @DisplayName("deleteValues - 리프레시 토큰 삭제")
    class DeleteValuesTest {

        @Test
        @DisplayName("성공: 키를 삭제하면 조회 시 빈 값을 반환한다")
        void success() {
            // given
            String key = "REFRESH:1:device";
            redisRepository.setValues(key, "token", Duration.ofMinutes(10));

            // when
            redisRepository.deleteValues(key);

            // then
            assertThat(redisRepository.getValues(key)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getValues - 리프레시 토큰 조회")
    class GetValuesTest {

        @Test
        @DisplayName("실패: 존재하지 않는 키를 조회하면 빈 Optional을 반환한다")
        void fail_keyNotFound() {
            // given
            String key = "REFRESH:999:unknown";

            // when
            Optional<String> value = redisRepository.getValues(key);

            // then
            assertThat(value).isEmpty();
        }
    }
}