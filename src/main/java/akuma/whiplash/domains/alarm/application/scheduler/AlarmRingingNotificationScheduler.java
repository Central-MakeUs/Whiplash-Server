package akuma.whiplash.domains.alarm.application.scheduler;

import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushInfo;
import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushTargetDto;
import akuma.whiplash.domains.alarm.domain.service.AlarmQueryService;
import akuma.whiplash.infrastructure.firebase.FcmService;
import akuma.whiplash.infrastructure.firebase.dto.FcmMetricResult;
import akuma.whiplash.infrastructure.redis.RedisService;
import akuma.whiplash.global.log.NoMethodLog;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmRingingNotificationScheduler {

    private final AlarmQueryService alarmQueryService;
    private final RedisService redisService;
    private final FcmService fcmService;
    private final MeterRegistry meterRegistry;

    private Counter ringingPushAttemptCounter;
    private Counter ringingPushSuccessCounter;
    private Counter ringingPushFailureCounter;
    //private Counter ringingInvalidTokenCounter;

    @PostConstruct
    void registerMetrics() {
        ringingPushAttemptCounter = meterRegistry.counter(
            "ringing_alarm.push_attempt", "scheduler", "alarm-ringing");

        ringingPushSuccessCounter = meterRegistry.counter(
            "ringing_alarm.push_success", "scheduler", "alarm-ringing");

        ringingPushFailureCounter = meterRegistry.counter(
            "ringing_alarm.push_failure", "scheduler", "alarm-ringing");

        /*ringingInvalidTokenCounter = meterRegistry.counter(
            "ringing_alarm.invalid_token_removed", "scheduler", "alarm-ringing");*/
    }

    // 10초 간격으로 실행
    @Scheduled(fixedRate = 10000, zone = "Asia/Seoul")
    @NoMethodLog
    public void sendRingingAlarmNotifications() {

        List<RingingPushInfo> infos = alarmQueryService.getRingingNotificationTargets();
        if (infos.isEmpty()) {
            return;
        }

        List<RingingPushTargetDto> targets = infos.stream()
            .flatMap(info -> redisService.getFcmTokens(info.memberId()).stream()
                .map(token -> RingingPushTargetDto.builder()
                    .token(token)
                    .alarmId(info.alarmId())
                    .memberId(info.memberId())
                    .build()))
            .toList();

        if (targets.isEmpty()) {
            return;
        }

        // 전송 시도 횟수 기록
        ringingPushAttemptCounter.increment(targets.size());

        log.info("알람 울림 푸시 알림 대상 {}건 전송 시도", targets.size());

        FcmMetricResult result = fcmService.sendRingingNotifications(targets);

        // 성공/실패 횟수 기록
        ringingPushSuccessCounter.increment(result.getSuccessCount());
        ringingPushFailureCounter.increment(result.getFailedCount());

        /*
        // 무효 토큰 제거된 개수 카운트
        if (!result.getInvalidTokens().isEmpty()) {
            ringingInvalidTokenCounter.increment(result.getInvalidTokens().size());
        }

        // Redis에서 무효 토큰 제거
        for (Map.Entry<Long, List<String>> e : result.getMemberToTokens().entrySet()) {
            Long memberId = e.getKey();
            e.getValue().stream()
                .filter(result.getInvalidTokens()::contains)
                .forEach(token -> redisService.removeInvalidToken(memberId, token));
        }
        */
    }
}
