package akuma.whiplash.infrastructure.redis;

import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushInfo;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 현재 울리고 있는 알람을 Redis Sorted Set으로 관리한다.
 *
 * <pre>
 * Key    : "alarm:ringing"
 * Member : "{alarmId}:{memberId}"
 * Score  : 알람 예정 시각의 epoch millis (Asia/Seoul 기준)
 * </pre>
 *
 * 스케줄러는 DB 풀스캔 대신 {@link #getRingingAlarms()}로 O(log N + K) 복잡도로
 * 현재 시각 이하의 항목만 추출하여 FCM 발송에 사용한다.
 */
@Repository
@RequiredArgsConstructor
public class RingingAlarmRedisRepository {

    private static final String RINGING_KEY = "alarm:ringing";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 알람이 울리기 시작했을 때 Sorted Set에 적재한다.
     * 동일 member(alarmId:memberId)로 재호출 시 score만 갱신되므로 멱등하다.
     *
     * @param alarmId          울리는 알람 ID
     * @param memberId         알람 소유자 ID
     * @param scoreEpochMillis 알람 예정 시각의 epoch millis (Asia/Seoul 기준)
     */
    public void add(Long alarmId, Long memberId, long scoreEpochMillis) {
        String member = alarmId + ":" + memberId;
        redisTemplate.opsForZSet().add(RINGING_KEY, member, scoreEpochMillis);
    }

    /**
     * 현재 시각 이하의 score를 가진 항목(= 지금 울려야 할 알람 전체)을 반환한다.
     * ZRANGEBYSCORE alarm:ringing 0 nowEpoch
     */
    public List<RingingPushInfo> getRingingAlarms() {
        long nowEpoch = System.currentTimeMillis();
        Set<String> members = redisTemplate.opsForZSet()
            .rangeByScore(RINGING_KEY, 0, nowEpoch);

        if (members == null || members.isEmpty()) {
            return List.of();
        }

        return members.stream()
            .map(member -> {
                String[] parts = member.split(":");
                return RingingPushInfo.builder()
                    .alarmId(Long.parseLong(parts[0]))
                    .memberId(Long.parseLong(parts[1]))
                    .build();
            })
            .toList();
    }

    /**
     * 알람이 비활성화(체크인·삭제)됐을 때 Sorted Set에서 제거한다.
     * 항목이 없으면 ZREM이 no-op이므로 항상 안전하게 호출할 수 있다.
     *
     * @param alarmId  알람 ID
     * @param memberId 알람 소유자 ID
     */
    public void remove(Long alarmId, Long memberId) {
        String member = alarmId + ":" + memberId;
        redisTemplate.opsForZSet().remove(RINGING_KEY, member);
    }
}
