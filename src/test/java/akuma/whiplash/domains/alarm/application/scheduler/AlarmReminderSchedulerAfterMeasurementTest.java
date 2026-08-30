package akuma.whiplash.domains.alarm.application.scheduler;

import static akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus.SCHEDULED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo;
import akuma.whiplash.domains.alarm.application.dto.etc.PushTargetDto;
import akuma.whiplash.domains.alarm.domain.service.AlarmCommandService;
import akuma.whiplash.domains.alarm.domain.service.AlarmQueryService;
import akuma.whiplash.domains.alarm.domain.service.AlarmQueryServiceImpl;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.domains.place.domain.client.GoogleClient;
import akuma.whiplash.domains.place.domain.client.impl.GoogleClientImpl;
import akuma.whiplash.global.util.date.TimeProvider;
import akuma.whiplash.infrastructure.firebase.FcmService;
import akuma.whiplash.infrastructure.firebase.dto.FcmSendResult;
import akuma.whiplash.infrastructure.redis.RedisService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Tag("measurement")
@DisplayName("AlarmReminderScheduler After Measurement")
class AlarmReminderSchedulerAfterMeasurementTest {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 8, 24, 12, 0);
    private static final int REPEAT_COUNT = 3;
    private static final Duration WORST_TARGET_LEAD_TIME = Duration.ofMinutes(59);
    private static final Duration MINIMUM_ALLOWED_LEAD_TIME = Duration.ofMinutes(55);

    private MockWebServer mockWebServer;
    private GoogleClient googleClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        googleClient = new GoogleClientImpl(
            WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                    HttpClient.create().responseTimeout(Duration.ofSeconds(3))
                ))
                .build(),
            "test-key",
            mockWebServer.url("/v4/geocode/location").toString(),
            mockWebServer.url("/").toString()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("sendPreAlarmNotifications - 사전 알림 정시성 After 측정")
    class PreNotificationTimelinessBoundaryTest {

        @Test
        @DisplayName("성공: cache miss 200건과 Google 지연 2초 조건에서도 Google 호출 없이 55분 전 기준을 지킨다")
        void success() {
            // given
            int targetCount = 200;
            int missCount = 200;
            Duration providerDelay = Duration.ofSeconds(2);
            Duration availableDelayBudget = WORST_TARGET_LEAD_TIME.minus(MINIMUM_ALLOWED_LEAD_TIME);

            // when
            List<Double> fcmStartSamples = new ArrayList<>();
            for (int repeat = 0; repeat < REPEAT_COUNT; repeat++) {
                Measurement measurement = measure(targetCount, missCount, providerDelay);
                assertMeasurementShape(measurement, targetCount);
                assertThat(measurement.googleRequestCount()).isZero();
                fcmStartSamples.add(measurement.fcmStartMillis());
            }
            double medianFcmStartMillis = median(fcmStartSamples);
            Duration measuredFcmStartDelay = Duration.ofMillis((long) medianFcmStartMillis);
            Duration remainingLeadTime = WORST_TARGET_LEAD_TIME.minus(measuredFcmStartDelay);

            // then
            assertThat(measuredFcmStartDelay).isLessThan(availableDelayBudget);
            assertThat(remainingLeadTime).isGreaterThanOrEqualTo(MINIMUM_ALLOWED_LEAD_TIME);
            System.out.printf(
                "AFTER scenario=timeliness-boundary target_count=%d miss_count=%d "
                    + "provider_delay_ms=%d fcm_start_ms=%.0f remaining_lead_seconds=%d "
                    + "allowed_min_lead_seconds=%d%n",
                targetCount,
                missCount,
                providerDelay.toMillis(),
                medianFcmStartMillis,
                remainingLeadTime.toSeconds(),
                MINIMUM_ALLOWED_LEAD_TIME.toSeconds()
            );
        }
    }

    private Measurement measure(int totalTargetCount, int missCount, Duration providerDelay) {
        int requestCountBefore = mockWebServer.getRequestCount();
        for (int index = 0; index < missCount; index++) {
            mockWebServer.enqueue(placeDetailsResponse(providerDelay));
        }

        AlarmRepository alarmRepository = mock(AlarmRepository.class);
        AlarmOccurrenceRepository occurrenceRepository = mock(AlarmOccurrenceRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        MemberDeviceRepository memberDeviceRepository = mock(MemberDeviceRepository.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        AlarmQueryService queryService = new AlarmQueryServiceImpl(
            alarmRepository,
            occurrenceRepository,
            memberRepository,
            memberDeviceRepository,
            timeProvider
        );

        List<OccurrencePushInfo> infos = java.util.stream.LongStream.rangeClosed(1, totalTargetCount)
            .mapToObj(id -> new OccurrencePushInfo(id, id, id))
            .toList();
        given(timeProvider.now()).willReturn(FIXED_NOW);
        given(occurrenceRepository.findPreNotificationTargetsByScheduledAtBetween(
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            eq(SCHEDULED)
        )).willReturn(infos);

        RedisService redisService = mock(RedisService.class);
        FcmService fcmService = mock(FcmService.class);
        AlarmCommandService commandService = mock(AlarmCommandService.class);
        given(redisService.getFcmTokens(anyLong())).willReturn(Set.of("test-token"));

        AtomicLong fcmStartedAtNanos = new AtomicLong(-1L);
        AtomicInteger fcmInvocationCount = new AtomicInteger();
        Map<Long, Integer> sendsByOccurrence = new HashMap<>();
        doAnswer(invocation -> {
            fcmStartedAtNanos.compareAndSet(-1L, System.nanoTime());
            fcmInvocationCount.incrementAndGet();
            List<PushTargetDto> targets = invocation.getArgument(0);
            targets.forEach(target -> sendsByOccurrence.merge(target.occurrenceId(), 1, Integer::sum));
            Set<Long> successOccurrenceIds = new HashSet<>();
            targets.forEach(target -> successOccurrenceIds.add(target.occurrenceId()));
            return FcmSendResult.builder()
                .successOccurrenceIds(successOccurrenceIds)
                .invalidTokens(List.of())
                .memberToTokens(Map.of())
                .successCount(targets.size())
                .failedCount(0)
                .build();
        }).when(fcmService).sendBulkNotification(anyList());

        AlarmReminderScheduler scheduler = new AlarmReminderScheduler(
            queryService,
            redisService,
            fcmService,
            commandService,
            new SimpleMeterRegistry()
        );
        scheduler.registerMetrics();

        long startedAtNanos = System.nanoTime();
        scheduler.sendPreAlarmNotifications();

        return new Measurement(
            TimeUnit.NANOSECONDS.toMillis(fcmStartedAtNanos.get() - startedAtNanos),
            mockWebServer.getRequestCount() - requestCountBefore,
            fcmInvocationCount.get(),
            Map.copyOf(sendsByOccurrence)
        );
    }

    private MockResponse placeDetailsResponse(Duration providerDelay) {
        return new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {
                  "id":"refreshed-place",
                  "formattedAddress":"서울",
                  "location":{"latitude":37.5,"longitude":127.0}
                }
                """)
            .setBodyDelay(providerDelay.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void assertMeasurementShape(Measurement measurement, int targetCount) {
        assertThat(measurement.fcmInvocationCount()).isEqualTo(1);
        assertThat(measurement.sendsByOccurrence()).hasSize(targetCount);
        assertThat(measurement.sendsByOccurrence().values()).containsOnly(1);
    }

    private double median(List<Double> samples) {
        List<Double> sorted = samples.stream().sorted().toList();
        return sorted.get(sorted.size() / 2);
    }

    private record Measurement(
        double fcmStartMillis,
        int googleRequestCount,
        int fcmInvocationCount,
        Map<Long, Integer> sendsByOccurrence
    ) {
    }
}
