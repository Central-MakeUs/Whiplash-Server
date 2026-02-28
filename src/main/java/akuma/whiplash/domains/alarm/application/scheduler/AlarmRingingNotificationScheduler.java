package akuma.whiplash.domains.alarm.application.scheduler;

import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushInfo;
import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushTargetDto;
import akuma.whiplash.domains.alarm.domain.service.AlarmQueryService;
import akuma.whiplash.infrastructure.firebase.FcmService;
import akuma.whiplash.infrastructure.firebase.dto.FcmMetricResult;
import akuma.whiplash.infrastructure.redis.RedisService;
import akuma.whiplash.global.log.NoMethodLog;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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

    // --- 카운터: FCM 발송 성공/실패 누적 ---
    private Counter ringingPushAttemptCounter;
    private Counter ringingPushSuccessCounter;
    private Counter ringingPushFailureCounter;

    // --- 타이머: 실행 시간 분포 측정 (p50, p95, p99, max 자동 산출) ---
    /** 스케줄러 전체 1회 실행 시간 */
    private Timer schedulerTotalTimer;
    /** DB 조회 구간만의 실행 시간 — DB 폴링이 병목임을 수치로 증명하기 위해 분리 */
    private Timer dbQueryTimer;
    /** FCM 발송 구간만의 실행 시간 */
    private Timer fcmSendTimer;

    // --- 게이지: 현재 ringing 대상 row 수 (스냅샷) ---
    /**
     * 매 실행 시점의 ringing 대상 수를 실시간으로 Prometheus에 노출.
     * row 수가 늘수록 DB 조회 시간과 함께 증가하는지 그라파나에서 상관관계 확인 용도.
     */
    private final AtomicInteger currentRingingTargetCount = new AtomicInteger(0);

    @PostConstruct
    void registerMetrics() {
        ringingPushAttemptCounter = meterRegistry.counter(
            "ringing_alarm.push_attempt", "scheduler", "alarm-ringing");
        ringingPushSuccessCounter = meterRegistry.counter(
            "ringing_alarm.push_success", "scheduler", "alarm-ringing");
        ringingPushFailureCounter = meterRegistry.counter(
            "ringing_alarm.push_failure", "scheduler", "alarm-ringing");

        schedulerTotalTimer = Timer.builder("ringing_alarm.scheduler_duration")
            .description("알람 울림 스케줄러 1회 전체 실행 시간")
            .publishPercentileHistogram()   // 그라파나에서 histogram_quantile()로 p95/p99 조회 가능
            .tag("scheduler", "alarm-ringing")
            .register(meterRegistry);

        dbQueryTimer = Timer.builder("ringing_alarm.db_query_duration")
            .description("findRingingNotificationTargets DB 조회 시간 — 폴링 병목 증명용")
            .publishPercentileHistogram()
            .tag("scheduler", "alarm-ringing")
            .register(meterRegistry);

        fcmSendTimer = Timer.builder("ringing_alarm.fcm_send_duration")
            .description("FCM 일괄 발송 시간")
            .publishPercentileHistogram()
            .tag("scheduler", "alarm-ringing")
            .register(meterRegistry);

        Gauge.builder("ringing_alarm.target_count", currentRingingTargetCount, AtomicInteger::get)
            .description("현재 실행 시점의 ringing 대상 alarm_occurrence row 수")
            .tag("scheduler", "alarm-ringing")
            .register(meterRegistry);
    }

    @Scheduled(fixedRate = 10000, zone = "Asia/Seoul")
    @NoMethodLog
    public void sendRingingAlarmNotifications() {
        schedulerTotalTimer.record(this::executeRingingNotification);
    }

    private void executeRingingNotification() {

        // ── 구간 1: DB 조회 ──────────────────────────────────────────────────
        List<RingingPushInfo> infos = dbQueryTimer.record(
            alarmQueryService::getRingingNotificationTargets
        );

        // 조회된 row 수를 게이지에 반영 (그라파나 상관관계 분석용)
        currentRingingTargetCount.set(infos.size());

        if (infos.isEmpty()) {
            return;
        }

        // ── 구간 2: Redis FCM 토큰 조회 + 발송 대상 조립 ─────────────────────
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

        ringingPushAttemptCounter.increment(targets.size());
        log.info("알람 울림 푸시 알림 대상 {}건 전송 시도", targets.size());

        // ── 구간 3: FCM 발송 ─────────────────────────────────────────────────
        FcmMetricResult result = fcmSendTimer.record(
            () -> fcmService.sendRingingNotifications(targets)
        );

        ringingPushSuccessCounter.increment(result.getSuccessCount());
        ringingPushFailureCounter.increment(result.getFailedCount());
    }
}
