package akuma.whiplash.infrastructure.redis;

import akuma.whiplash.common.config.RedisContainerInitializer;
import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RingingAlarmRedisRepository Test")
@DataRedisTest
@Import(RingingAlarmRedisRepository.class)
@ContextConfiguration(initializers = RedisContainerInitializer.class)
class RingingAlarmRedisRepositoryTest {

    @Autowired
    private RingingAlarmRedisRepository ringingAlarmRedisRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        // 테스트 간 Sorted Set 상태 초기화
        redisTemplate.delete("alarm:ringing");
    }

    @Nested
    @DisplayName("add - 울리는 알람 Sorted Set 적재")
    class AddTest {

        @Test
        @DisplayName("성공: 알람 정보가 [alarmId:memberId / score=epoch millis] 형태로 적재된다")
        void success() {
            // given
            long score = System.currentTimeMillis() - 1000; // 과거 시각 → 조회 대상

            // when
            ringingAlarmRedisRepository.add(42L, 7L, score);

            // then
            List<RingingPushInfo> result = ringingAlarmRedisRepository.getRingingAlarms();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).alarmId()).isEqualTo(42L);
            assertThat(result.get(0).memberId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("성공: 동일한 alarmId:memberId로 재호출 시 score만 갱신된다(멱등)")
        void success_idempotent() {
            // given
            long firstScore = System.currentTimeMillis() - 2000;
            long updatedScore = System.currentTimeMillis() - 1000;
            ringingAlarmRedisRepository.add(42L, 7L, firstScore);

            // when
            ringingAlarmRedisRepository.add(42L, 7L, updatedScore); // 동일 member, score만 갱신

            // then
            List<RingingPushInfo> result = ringingAlarmRedisRepository.getRingingAlarms();
            assertThat(result).hasSize(1); // 중복 적재 아님
        }
    }

    @Nested
    @DisplayName("getRingingAlarms - 현재 시각 이하의 알람 조회")
    class GetRingingAlarmsTest {

        @Test
        @DisplayName("성공: score가 현재 시각 이하인 알람만 반환한다")
        void success() {
            // given
            long pastScore = System.currentTimeMillis() - 1000;   // 과거 → 반환 대상
            long futureScore = System.currentTimeMillis() + 60_000; // 미래 → 반환 제외
            ringingAlarmRedisRepository.add(42L, 7L, pastScore);
            ringingAlarmRedisRepository.add(99L, 3L, futureScore);

            // when
            List<RingingPushInfo> result = ringingAlarmRedisRepository.getRingingAlarms();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).alarmId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("성공: score가 현재 시각 이후면 반환하지 않는다")
        void success_futureScoreNotReturned() {
            // given
            long futureScore = System.currentTimeMillis() + 60_000;
            ringingAlarmRedisRepository.add(42L, 7L, futureScore);

            // when
            List<RingingPushInfo> result = ringingAlarmRedisRepository.getRingingAlarms();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("remove - 울리는 알람 Sorted Set 제거")
    class RemoveTest {

        @Test
        @DisplayName("성공: ZREM 후 해당 알람이 조회되지 않는다")
        void success() {
            // given
            long score = System.currentTimeMillis() - 1000;
            ringingAlarmRedisRepository.add(42L, 7L, score);

            // when
            ringingAlarmRedisRepository.remove(42L, 7L);

            // then
            List<RingingPushInfo> result = ringingAlarmRedisRepository.getRingingAlarms();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("성공: 존재하지 않는 항목을 제거해도 예외가 발생하지 않는다(no-op)")
        void success_removeNonExistentIsNoOp() {
            // given (아무것도 적재하지 않음)

            // when & then
            ringingAlarmRedisRepository.remove(999L, 999L); // 예외 없이 통과해야 함
        }
    }
}
