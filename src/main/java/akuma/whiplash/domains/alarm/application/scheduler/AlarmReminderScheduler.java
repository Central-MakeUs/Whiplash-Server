package akuma.whiplash.domains.alarm.application.scheduler;

import akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo;
import akuma.whiplash.domains.alarm.application.dto.etc.PushTargetDto;
import akuma.whiplash.domains.alarm.domain.service.AlarmCommandService;
import akuma.whiplash.domains.alarm.domain.service.AlarmQueryService;
import akuma.whiplash.infrastructure.firebase.FcmService;
import akuma.whiplash.infrastructure.firebase.dto.FcmSendResult;
import akuma.whiplash.infrastructure.redis.RedisService;
import akuma.whiplash.global.log.NoMethodLog;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmReminderScheduler {

    private final AlarmQueryService alarmQueryService;
    private final RedisService redisService;
    private final FcmService fcmService;
    private final AlarmCommandService alarmCommandService;
    private final MeterRegistry meterRegistry;

    private Counter preAlarmPushAttemptCounter;
    private Counter preAlarmPushSuccessCounter;
    private Counter preAlarmPushFailureCounter;
    private Counter invalidFcmTokenCounter;

    @PostConstruct
    void registerMetrics() {
        preAlarmPushAttemptCounter = meterRegistry.counter(
            "pre_alarm.push_attempt", "scheduler", "pre-alarm");

        preAlarmPushSuccessCounter = meterRegistry.counter(
            "pre_alarm.push_success", "scheduler", "pre-alarm");

        preAlarmPushFailureCounter = meterRegistry.counter(
            "pre_alarm.push_failure", "scheduler", "pre-alarm");

        invalidFcmTokenCounter = meterRegistry.counter(
            "pre_alarm.invalid_token_removed", "scheduler", "pre-alarm");
    }

    // 매 분 마다 실행
    @Scheduled(cron = "0 * * * * *")
    @NoMethodLog
    public void sendPreAlarmNotifications() {

        // 0. 시간 기준 (분 단위 정렬)
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime windowStart = now.plusMinutes(59);
        LocalDateTime windowEnd = now.plusMinutes(61);

        // 1. DB에서 알림 대상 필터링 (자정 크로스 안전)
        List<OccurrencePushInfo> infos = alarmQueryService.getPreNotificationTargets(windowStart, windowEnd);

        if (infos.isEmpty())
            return;

        // 2. Redis에서 FCM 토큰 조회 → push 대상 생성
        List<PushTargetDto> targets = infos.stream()
            .flatMap(info ->
                redisService.getFcmTokens(info.memberId()).stream()
                    .map(token -> PushTargetDto.builder()
                        .token(token)
                        .address(info.address())
                        .memberId(info.memberId())
                        .occurrenceId(info.occurrenceId())
                        .build()
                    )
            )
            .toList();

        if (targets.isEmpty())
            return;

        // 실제 전송 시도한 횟수 기록
        preAlarmPushAttemptCounter.increment(targets.size());

        log.info("알람 울리기 1시간 전 푸시 알림 대상 {}건 전송 시도", targets.size());

        // 3. FCM 전송
        FcmSendResult result = fcmService.sendBulkNotification(targets);

        // 전송 성공/실패 횟수 기록
        preAlarmPushSuccessCounter.increment(result.getSuccessCount());
        preAlarmPushFailureCounter.increment(result.getFailedCount());

        // 4. 성공한 occurrence만 reminderSent=true 벌크 업데이트
        if (!result.getSuccessOccurrenceIds().isEmpty()) {
            alarmCommandService.markReminderSent(result.getSuccessOccurrenceIds());
        }

        // 5. 무효 토큰 정리: 각 회원의 토큰 중 무효한 것만 제거
        Set<String> invalidTokenSet = new HashSet<>(result.getInvalidTokens());
        int invalidCount = 0;

        for (Map.Entry<Long, List<String>> e : result.getMemberToTokens().entrySet()) {
            Long memberId = e.getKey();
            invalidCount += (int) e.getValue().stream()
                .filter(invalidTokenSet::contains)
                .peek(token -> redisService.removeInvalidToken(memberId, token))
                .count();
        }

        // 제거된 무효 토큰 개수 기록
        if (invalidCount > 0) {
            invalidFcmTokenCounter.increment(invalidCount);
        }
    }

}
