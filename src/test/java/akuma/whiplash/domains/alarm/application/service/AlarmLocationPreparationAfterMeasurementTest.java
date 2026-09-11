package akuma.whiplash.domains.alarm.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.dto.response.LocationPreparationResponse;
import akuma.whiplash.domains.alarm.application.dto.response.LocationPreparationState;
import akuma.whiplash.domains.alarm.domain.constant.LocationSource;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.domain.service.AlarmLocationCachePersistenceService;
import akuma.whiplash.domains.alarm.domain.service.AlarmLocationCacheService;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.place.domain.client.impl.GoogleClientImpl;
import akuma.whiplash.global.util.date.TimeProvider;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Tag("measurement")
@ExtendWith(MockitoExtension.class)
@DisplayName("인증 화면 위치 준비 After 측정")
class AlarmLocationPreparationAfterMeasurementTest {

    private static final int REQUEST_COUNT = 20;
    private static final Duration GOOGLE_DELAY = Duration.ofSeconds(2);

    @Mock
    private AlarmRepository alarmRepository;
    @Mock
    private AlarmOccurrenceRepository occurrenceRepository;
    @Mock
    private TaskScheduler taskScheduler;

    private MockWebServer mockWebServer;
    private ThreadPoolTaskExecutor executor;
    private AlarmLocationPreparationService service;
    private Map<Long, AlarmEntity> alarms;
    private TimeProvider timeProvider;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        timeProvider = new TimeProvider();
        alarms = new HashMap<>();
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(16);
        executor.initialize();

        WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(Duration.ofSeconds(10))))
            .build();
        GoogleClientImpl googleClient = new GoogleClientImpl(
            webClient,
            "test-key",
            mockWebServer.url("/v4/geocode/location").toString(),
            mockWebServer.url("/").toString()
        );
        AlarmLocationCachePersistenceService persistence = new AlarmLocationCachePersistenceService(alarmRepository);
        AlarmLocationCacheService cacheService = new AlarmLocationCacheService(
            googleClient, timeProvider, alarmRepository, persistence
        );
        service = new AlarmLocationPreparationService(
            alarmRepository, occurrenceRepository, cacheService, timeProvider, executor, taskScheduler,
            new SimpleMeterRegistry()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        executor.shutdown();
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("실제 서비스 경로에서 초기 요청은 즉시 반환되고 20개 Google 호출은 worker 16개로 완료된다")
    void measureActualAfterPath() throws Exception {
        List<AlarmOccurrenceEntity> occurrences = prepareFixtures(REQUEST_COUNT);
        enqueueSuccessResponses(REQUEST_COUNT);

        long requestStartedAt = System.nanoTime();
        List<Long> initialLatencies = new ArrayList<>();
        for (AlarmOccurrenceEntity occurrence : occurrences) {
            long startedAt = System.nanoTime();
            LocationPreparationResponse response = service.getLocationPreparation(
                occurrence.getAlarm().getMember().getId(), occurrence.getAlarm().getId(), occurrence.getId()
            );
            initialLatencies.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
            assertThat(response.state()).isEqualTo(LocationPreparationState.PREPARING);
        }

        waitUntilReady(occurrences, Duration.ofSeconds(15));
        initialLatencies.sort(Long::compareTo);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - requestStartedAt);
        System.out.printf(
            "location-preparation after actual-http workers=16 queue=16 requests=%d initial_p50=%dms initial_p95=%dms total=%dms google_calls=%d%n",
            REQUEST_COUNT, percentile(initialLatencies, .50), percentile(initialLatencies, .95), elapsedMillis,
            mockWebServer.getRequestCount()
        );

        assertThat(initialLatencies.get(initialLatencies.size() - 1)).isLessThan(500L);
        assertThat(mockWebServer.getRequestCount()).isEqualTo(REQUEST_COUNT);
    }

    @Test
    @DisplayName("48건 burst는 16 실행 + 16 대기 후 초과분을 BUSY로 거절한다")
    void measureSaturation() throws Exception {
        int burst = 48;
        List<AlarmOccurrenceEntity> occurrences = prepareFixtures(burst);
        enqueueSuccessResponses(32);
        List<LocationPreparationState> states = new ArrayList<>();
        for (AlarmOccurrenceEntity occurrence : occurrences) {
            states.add(service.getLocationPreparation(
                occurrence.getAlarm().getMember().getId(), occurrence.getAlarm().getId(), occurrence.getId()
            ).state());
        }
        long busyCount = states.stream().filter(state -> state == LocationPreparationState.BUSY).count();
        waitForRequestCount(32, Duration.ofSeconds(5));
        System.out.printf("location-preparation saturation workers=16 queue=16 burst=%d busy=%d accepted=%d%n",
            burst, busyCount, burst - busyCount);
        assertThat(busyCount).isEqualTo(16);
        assertThat(mockWebServer.getRequestCount()).isEqualTo(32);
    }

    private List<AlarmOccurrenceEntity> prepareFixtures(int count) {
        LocalDateTime now = timeProvider.now();
        List<AlarmOccurrenceEntity> occurrences = new ArrayList<>();
        for (long id = 1; id <= count; id++) {
            AlarmEntity alarm = AlarmEntity.builder()
                .id(id)
                .alarmPurpose("measurement")
                .locationSource(LocationSource.GOOGLE_PLACE)
                .googlePlaceId("place-" + id)
                .member(MemberFixture.MEMBER_1.toMockEntity())
                .build();
            alarm.updateGooglePlaceLocation("place-" + id, "expired", 37.5, 127.0, now.minusDays(29));
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceEntity.builder()
                .id(1_000L + id)
                .alarm(alarm)
                .scheduledAt(now.plusHours(1))
                .occurrenceDate(now.toLocalDate())
                .occurrenceTime(now.toLocalTime().plusHours(1))
                .status(OccurrenceStatus.SCHEDULED)
                .build();
            alarms.put(id, alarm);
            occurrences.add(occurrence);
        }
        given(alarmRepository.findByIdWithMember(org.mockito.ArgumentMatchers.anyLong()))
            .willAnswer(invocation -> Optional.of(alarms.get(invocation.getArgument(0))));
        given(alarmRepository.findById(org.mockito.ArgumentMatchers.anyLong()))
            .willAnswer(invocation -> Optional.of(alarms.get(invocation.getArgument(0))));
        given(occurrenceRepository.findByIdAndAlarmIdForLocationPreparation(
            org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
            .willAnswer(invocation -> occurrences.stream()
                .filter(item -> item.getId().equals(invocation.getArgument(0)))
                .findFirst());
        return occurrences;
    }

    private void enqueueSuccessResponses(int count) {
        for (int index = 0; index < count; index++) {
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBodyDelay(GOOGLE_DELAY.toMillis(), TimeUnit.MILLISECONDS)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"place\",\"formattedAddress\":\"서울특별시\",\"location\":{\"latitude\":37.5,\"longitude\":127.0}}"));
        }
    }

    private void waitUntilReady(List<AlarmOccurrenceEntity> occurrences, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            long ready = occurrences.stream().filter(item -> {
                LocationPreparationResponse response = service.getLocationPreparation(
                    item.getAlarm().getMember().getId(), item.getAlarm().getId(), item.getId());
                return response.state() == LocationPreparationState.READY;
            }).count();
            if (ready == occurrences.size()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("location preparation did not become READY within " + timeout);
    }

    private void waitForRequestCount(int expected, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (mockWebServer.getRequestCount() < expected && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
    }

    private long percentile(List<Long> values, double percentile) {
        int index = (int) Math.ceil(percentile * values.size()) - 1;
        return values.get(index);
    }
}
