package akuma.whiplash.infrastructure.firebase;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.domains.alarm.application.dto.etc.PushTargetDto;
import akuma.whiplash.infrastructure.redis.RedisService;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.SendResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@Tag("measurement")
@DisplayName("FcmService Dispatch Measurement")
class FcmServiceDispatchMeasurementTest {

    private static final int TARGET_COUNT = 200;
    private static final int REPEAT_COUNT = 3;
    private static final Duration SIMULATED_FCM_RESPONSE_DELAY = Duration.ofMillis(25);
    private static final Duration WORST_TARGET_LEAD_TIME = Duration.ofMinutes(59);
    private static final Duration MINIMUM_ALLOWED_LEAD_TIME = Duration.ofMinutes(55);

    @Test
    @DisplayName("성공: 서로 다른 occurrence 200건의 마지막 FCM 요청 완료 시점이 55분 전 하한을 지킨다")
    void success() throws Exception {
        // given
        Duration availableDelayBudget = WORST_TARGET_LEAD_TIME.minus(MINIMUM_ALLOWED_LEAD_TIME);
        List<Long> completionSamples = new ArrayList<>();

        // when
        for (int repeat = 0; repeat < REPEAT_COUNT; repeat++) {
            Measurement measurement = measure();
            assertThat(measurement.fcmCallCount()).isEqualTo(TARGET_COUNT);
            completionSamples.add(measurement.lastFcmCompletedMillis());
        }
        long medianLastFcmCompletedMillis = median(completionSamples);
        Duration lastFcmCompletedDelay = Duration.ofMillis(medianLastFcmCompletedMillis);
        Duration remainingLeadTime = WORST_TARGET_LEAD_TIME.minus(lastFcmCompletedDelay);

        // then
        assertThat(lastFcmCompletedDelay).isLessThan(availableDelayBudget);
        assertThat(remainingLeadTime).isGreaterThanOrEqualTo(MINIMUM_ALLOWED_LEAD_TIME);
        System.out.printf(
            "AFTER scenario=fcm-dispatch-boundary target_count=%d simulated_fcm_delay_ms=%d "
                + "fcm_call_count=%d last_fcm_completed_ms=%d remaining_lead_seconds=%d "
                + "allowed_min_lead_seconds=%d%n",
            TARGET_COUNT,
            SIMULATED_FCM_RESPONSE_DELAY.toMillis(),
            TARGET_COUNT,
            medianLastFcmCompletedMillis,
            remainingLeadTime.toSeconds(),
            MINIMUM_ALLOWED_LEAD_TIME.toSeconds()
        );
    }

    private Measurement measure() throws Exception {
        FirebaseMessaging firebaseMessaging = Mockito.mock(FirebaseMessaging.class);
        BatchResponse batchResponse = successfulBatchResponse();
        AtomicInteger fcmCallCount = new AtomicInteger();
        AtomicLong lastFcmCompletedAtNanos = new AtomicLong(-1L);

        try (MockedStatic<FirebaseMessaging> firebaseMessagingStatic = Mockito.mockStatic(FirebaseMessaging.class)) {
            firebaseMessagingStatic.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            Mockito.doAnswer(invocation -> {
                fcmCallCount.incrementAndGet();
                Thread.sleep(SIMULATED_FCM_RESPONSE_DELAY.toMillis());
                lastFcmCompletedAtNanos.set(System.nanoTime());
                return batchResponse;
            }).when(firebaseMessaging).sendEachForMulticast(Mockito.any());

            FcmService fcmService = new FcmService(Mockito.mock(RedisService.class));
            long startedAtNanos = System.nanoTime();
            fcmService.sendBulkNotification(targets());

            return new Measurement(
                fcmCallCount.get(),
                TimeUnit.NANOSECONDS.toMillis(lastFcmCompletedAtNanos.get() - startedAtNanos)
            );
        }
    }

    private BatchResponse successfulBatchResponse() {
        BatchResponse batchResponse = Mockito.mock(BatchResponse.class);
        SendResponse sendResponse = Mockito.mock(SendResponse.class);
        Mockito.when(sendResponse.isSuccessful()).thenReturn(true);
        Mockito.when(batchResponse.getResponses()).thenReturn(List.of(sendResponse));
        Mockito.when(batchResponse.getSuccessCount()).thenReturn(1);
        Mockito.when(batchResponse.getFailureCount()).thenReturn(0);
        return batchResponse;
    }

    private List<PushTargetDto> targets() {
        return java.util.stream.LongStream.rangeClosed(1, TARGET_COUNT)
            .mapToObj(id -> PushTargetDto.builder()
                .token("test-token-" + id)
                .memberId(id)
                .alarmId(id)
                .occurrenceId(id)
                .build()
            )
            .toList();
    }

    private long median(List<Long> samples) {
        List<Long> sorted = samples.stream().sorted().toList();
        return sorted.get(sorted.size() / 2);
    }

    private record Measurement(int fcmCallCount, long lastFcmCompletedMillis) {
    }
}
