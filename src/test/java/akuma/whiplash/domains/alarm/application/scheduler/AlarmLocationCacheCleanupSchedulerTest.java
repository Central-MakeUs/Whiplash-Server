package akuma.whiplash.domains.alarm.application.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import akuma.whiplash.domains.alarm.domain.service.AlarmLocationCacheCleanupService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlarmLocationCacheCleanupScheduler Unit Test")
class AlarmLocationCacheCleanupSchedulerTest {

    @Mock
    private AlarmLocationCacheCleanupService alarmLocationCacheCleanupService;

    private SimpleMeterRegistry meterRegistry;
    private AlarmLocationCacheCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        scheduler = new AlarmLocationCacheCleanupScheduler(alarmLocationCacheCleanupService, meterRegistry);
        scheduler.registerMetrics();
    }

    @Nested
    @DisplayName("clearExpiredGoogleLocationCaches - 만료 캐시 정리")
    class ClearExpiredGoogleLocationCachesTest {

        @Test
        @DisplayName("성공: 삭제 건수와 마지막 성공 시각을 기록한다")
        void success() {
            // given
            given(alarmLocationCacheCleanupService.clearExpiredGoogleLocationCaches()).willReturn(3);

            // when
            scheduler.clearExpiredGoogleLocationCaches();

            // then
            assertThat(meterRegistry.get("alarm.location_cache.cleared").counter().count()).isEqualTo(3);
            assertThat(meterRegistry.get("alarm.location_cache.last_success_epoch_seconds").gauge().value()).isPositive();
        }

        @Test
        @DisplayName("성공: 정리 실패를 실패 지표로 기록하고 예외를 다시 던진다")
        void success_recordsFailureMetric() {
            // given
            given(alarmLocationCacheCleanupService.clearExpiredGoogleLocationCaches())
                .willThrow(new IllegalStateException("database unavailable"));

            // when
            org.assertj.core.api.Assertions.assertThatThrownBy(scheduler::clearExpiredGoogleLocationCaches)
                .isInstanceOf(IllegalStateException.class);

            // then
            assertThat(meterRegistry.get("alarm.location_cache.cleanup_failure").counter().count()).isEqualTo(1);
        }
    }
}
