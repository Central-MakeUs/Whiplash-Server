package akuma.whiplash.domains.alarm.application.scheduler;

import akuma.whiplash.domains.alarm.domain.service.AlarmLocationCacheCleanupService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmLocationCacheCleanupScheduler {

    private final AlarmLocationCacheCleanupService alarmLocationCacheCleanupService;
    private final MeterRegistry meterRegistry;

    private Counter clearedLocationCacheCounter;
    private Counter cleanupSuccessCounter;
    private Counter cleanupFailureCounter;
    private Timer cleanupTimer;
    private final AtomicLong lastSuccessEpochSeconds = new AtomicLong();

    @PostConstruct
    void registerMetrics() {
        clearedLocationCacheCounter = meterRegistry.counter(
            "alarm.location_cache.cleared", "source", "google_place"
        );
        cleanupSuccessCounter = meterRegistry.counter("alarm.location_cache.cleanup_success");
        cleanupFailureCounter = meterRegistry.counter("alarm.location_cache.cleanup_failure");
        cleanupTimer = Timer.builder("alarm.location_cache.cleanup_duration")
            .description("Google 장소 위치 캐시 정리 작업 실행 시간")
            .publishPercentileHistogram()
            .register(meterRegistry);
        Gauge.builder("alarm.location_cache.last_success_epoch_seconds", lastSuccessEpochSeconds, AtomicLong::get)
            .description("Google 장소 위치 캐시 정리 작업의 마지막 성공 시각")
            .register(meterRegistry);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void removeExpiredGoogleLocationCaches() {
        cleanupTimer.record(() -> {
            try {
                int clearedCount = alarmLocationCacheCleanupService.removeExpiredGoogleLocationCaches();
                clearedLocationCacheCounter.increment(clearedCount);
                cleanupSuccessCounter.increment();
                lastSuccessEpochSeconds.set(Instant.now().getEpochSecond());
                log.info("만료된 Google 장소 위치 캐시 {}건을 삭제했습니다.", clearedCount);
            } catch (RuntimeException exception) {
                cleanupFailureCounter.increment();
                log.error("Google 장소 위치 캐시 정리에 실패했습니다.", exception);
                throw exception;
            }
        });
    }
}
