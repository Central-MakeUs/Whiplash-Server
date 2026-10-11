package akuma.whiplash.domains.alarm.application.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo;
import akuma.whiplash.domains.alarm.domain.service.AlarmCommandService;
import akuma.whiplash.domains.alarm.domain.service.AlarmQueryService;
import akuma.whiplash.infrastructure.firebase.FcmService;
import akuma.whiplash.infrastructure.firebase.dto.FcmSendResult;
import akuma.whiplash.infrastructure.redis.RedisService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlarmReminderScheduler 단위 테스트")
class AlarmReminderSchedulerTest {

    @Mock
    private AlarmQueryService alarmQueryService;
    @Mock
    private RedisService redisService;
    @Mock
    private FcmService fcmService;
    @Mock
    private AlarmCommandService alarmCommandService;

    private AlarmReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AlarmReminderScheduler(
            alarmQueryService,
            redisService,
            fcmService,
            alarmCommandService,
            new SimpleMeterRegistry()
        );
        scheduler.registerMetrics();
    }

    @Nested
    @DisplayName("sendPreAlarmNotifications - 사전 알림 무효 토큰 정리")
    class SendPreAlarmNotificationsTest {

        @Test
        @DisplayName("성공: 등록 해제된 토큰만 해당 회원의 Redis에서 제거한다")
        void success() {
            // given
            given(alarmQueryService.getPreNotificationTargets(
                any(LocalDateTime.class), any(LocalDateTime.class)
            )).willReturn(List.of(new OccurrencePushInfo(100L, 1L, 10L)));
            given(redisService.getFcmTokens(1L)).willReturn(Set.of("stale-token", "valid-token"));
            given(fcmService.sendBulkNotification(anyList())).willReturn(FcmSendResult.builder()
                .successOccurrenceIds(Set.of(100L))
                .invalidTokens(List.of("stale-token"))
                .memberToTokens(Map.of(1L, List.of("stale-token", "valid-token")))
                .successCount(1)
                .failedCount(1)
                .build());

            // when
            scheduler.sendPreAlarmNotifications();

            // then
            verify(redisService).removeInvalidToken(1L, "stale-token");
            verify(redisService, never()).removeInvalidToken(1L, "valid-token");
        }
    }
}
