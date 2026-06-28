package akuma.whiplash.domains.alarm.application.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushInfo;
import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushTargetDto;
import akuma.whiplash.domains.alarm.domain.service.AlarmQueryService;
import akuma.whiplash.infrastructure.firebase.FcmService;
import akuma.whiplash.infrastructure.firebase.dto.FcmMetricResult;
import akuma.whiplash.infrastructure.redis.RedisService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlarmRingingNotificationScheduler Unit Test")
class AlarmRingingNotificationSchedulerTest {

    @Mock
    private AlarmQueryService alarmQueryService;
    @Mock
    private RedisService redisService;
    @Mock
    private FcmService fcmService;

    private AlarmRingingNotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AlarmRingingNotificationScheduler(
            alarmQueryService,
            redisService,
            fcmService,
            new SimpleMeterRegistry()
        );
        scheduler.registerMetrics();
    }

    @Nested
    @DisplayName("sendRingingAlarmNotifications - 알람 울림 푸시 발송")
    class SendRingingAlarmNotificationsTest {

        @Test
        @DisplayName("성공: DB 조회 결과가 비어 있으면 FCM을 발송하지 않는다")
        void success_emptyTargets() {
            // given
            given(alarmQueryService.getRingingNotificationTargets()).willReturn(List.of());

            // when
            scheduler.sendRingingAlarmNotifications();

            // then
            verifyNoInteractions(redisService);
            verify(fcmService, never()).sendRingingNotifications(anyList());
        }

        @Test
        @DisplayName("성공: DB 조회 결과와 FCM 토큰으로 울림 푸시 대상을 만들어 발송한다")
        @SuppressWarnings({"rawtypes", "unchecked"})
        void success() {
            // given
            RingingPushInfo firstInfo = new RingingPushInfo(1L, 10L);
            RingingPushInfo secondInfo = new RingingPushInfo(2L, 20L);
            given(alarmQueryService.getRingingNotificationTargets())
                .willReturn(List.of(firstInfo, secondInfo));
            given(redisService.getFcmTokens(10L)).willReturn(Set.of("token-a", "token-b"));
            given(redisService.getFcmTokens(20L)).willReturn(Set.of("token-c"));
            given(fcmService.sendRingingNotifications(anyList())).willReturn(FcmMetricResult.builder()
                .successCount(3)
                .failedCount(0)
                .build());

            // when
            scheduler.sendRingingAlarmNotifications();

            // then
            ArgumentCaptor<List<RingingPushTargetDto>> targetsCaptor = ArgumentCaptor.forClass((Class) List.class);
            verify(fcmService).sendRingingNotifications(targetsCaptor.capture());
            List<RingingPushTargetDto> targets = targetsCaptor.getValue();
            assertThat(targets)
                .extracting(RingingPushTargetDto::alarmId, RingingPushTargetDto::memberId, RingingPushTargetDto::token)
                .containsExactlyInAnyOrder(
                    tuple(1L, 10L, "token-a"),
                    tuple(1L, 10L, "token-b"),
                    tuple(2L, 20L, "token-c")
                );
        }
    }
}
