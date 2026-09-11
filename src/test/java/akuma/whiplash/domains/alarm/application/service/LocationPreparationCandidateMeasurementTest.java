package akuma.whiplash.domains.alarm.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.domains.place.domain.client.GoogleClient;
import akuma.whiplash.domains.place.domain.client.impl.GoogleClientImpl;
import akuma.whiplash.domains.place.domain.model.PlaceDetailsCriteria;
import akuma.whiplash.global.exception.ApplicationException;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Tag("measurement")
@DisplayName("인증 화면 위치 준비 후보 측정")
class LocationPreparationCandidateMeasurementTest {

    private static final int UNIQUE_MISS_COUNT = 20;
    private static final Duration GOOGLE_DELAY = Duration.ofSeconds(2);
    private static final Duration GOOGLE_TIMEOUT = Duration.ofSeconds(10);

    private MockWebServer mockWebServer;
    private GoogleClient googleClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create().responseTimeout(GOOGLE_TIMEOUT)
            ))
            .build();
        googleClient = new GoogleClientImpl(
            webClient,
            "test-key",
            mockWebServer.url("/v4/geocode/location").toString(),
            mockWebServer.url("/").toString()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("후보 worker 4·8·16에서 20개 cache miss의 실제 HTTP blocking 완료 시간을 비교한다")
    void measure_workerCandidates() throws Exception {
        // given
        List<WorkerMeasurement> measurements = new ArrayList<>();
        for (int workerCount : List.of(4, 8, 16)) {
            measurements.add(measureWorkers(workerCount));
        }

        // when
        measurements.forEach(measurement -> System.out.printf(
            "location-preparation worker=%d, requests=%d, p50=%dms, p95=%dms, max=%dms%n",
            measurement.workerCount(),
            measurement.requestCount(),
            measurement.p50Millis(),
            measurement.p95Millis(),
            measurement.maxMillis()
        ));

        // then
        assertThat(measurements).allSatisfy(measurement ->
            assertThat(measurement.requestCount()).isEqualTo(UNIQUE_MISS_COUNT)
        );
        assertThat(mockWebServer.getRequestCount()).isEqualTo(UNIQUE_MISS_COUNT * 3);
    }

    @Test
    @DisplayName("재시도 횟수별 자동 회복 범위와 10초 timeout 최악 시간을 비교한다")
    void measure_retryCandidates() throws Exception {
        // given
        List<RetryMeasurement> measurements = new ArrayList<>();
        for (int retryCount : List.of(0, 1, 2)) {
            measurements.add(measureRetries(retryCount));
        }

        // when
        measurements.forEach(measurement -> System.out.printf(
            "location-preparation retries=%d, providerCalls=%d, ready=%s, elapsed=%dms, timeoutUpperBound=%ds%n",
            measurement.retryCount(),
            measurement.providerCallCount(),
            measurement.ready(),
            measurement.elapsedMillis(),
            measurement.timeoutUpperBoundSeconds()
        ));

        // then
        assertThat(measurements).extracting(RetryMeasurement::providerCallCount)
            .containsExactly(1, 2, 3);
        assertThat(measurements).extracting(RetryMeasurement::ready)
            .containsExactly(false, false, true);
        assertThat(measurements).extracting(RetryMeasurement::timeoutUpperBoundSeconds)
            .containsExactly(10L, 20L, 30L);
    }

    private WorkerMeasurement measureWorkers(int workerCount) throws Exception {
        enqueueSuccessResponses(UNIQUE_MISS_COUNT, GOOGLE_DELAY);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            workerCount,
            workerCount,
            0,
            TimeUnit.MILLISECONDS,
            // Measurement-only capacity: it accepts this 20-item burst so worker count is the sole variable.
            new ArrayBlockingQueue<>(UNIQUE_MISS_COUNT)
        );
        long startedAt = System.nanoTime();
        try {
            List<Future<Long>> futures = new ArrayList<>();
            for (int index = 0; index < UNIQUE_MISS_COUNT; index++) {
                futures.add(executor.submit(requestPlaceDetails(startedAt)));
            }

            List<Long> completionMillis = new ArrayList<>();
            for (Future<Long> future : futures) {
                completionMillis.add(future.get(30, TimeUnit.SECONDS));
            }
            completionMillis.sort(Long::compareTo);
            return new WorkerMeasurement(
                workerCount,
                completionMillis.size(),
                percentile(completionMillis, 0.50),
                percentile(completionMillis, 0.95),
                completionMillis.get(completionMillis.size() - 1)
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private RetryMeasurement measureRetries(int retryCount) throws Exception {
        enqueueFailureResponses(Math.min(retryCount + 1, 2));
        if (retryCount >= 2) {
            enqueueSuccessResponses(1, Duration.ofMillis(300));
        }
        long startedAt = System.nanoTime();
        int providerCallCount = 0;
        boolean ready = false;
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            providerCallCount++;
            try {
                googleClient.getPlaceDetails(new PlaceDetailsCriteria("place-id", null, "ko", "KR"));
                ready = true;
                break;
            } catch (ApplicationException ignored) {
                // The candidate comparison intentionally retries the same retryable fixture only.
            }
        }
        return new RetryMeasurement(
            retryCount,
            providerCallCount,
            ready,
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt),
            GOOGLE_TIMEOUT.toSeconds() * (retryCount + 1L)
        );
    }

    private Callable<Long> requestPlaceDetails(long startedAt) {
        return () -> {
            googleClient.getPlaceDetails(new PlaceDetailsCriteria("place-id", null, "ko", "KR"));
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        };
    }

    private void enqueueSuccessResponses(int count, Duration delay) {
        for (int index = 0; index < count; index++) {
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBodyDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                    {"id":"place-id","formattedAddress":"서울특별시","location":{"latitude":37.5,"longitude":127.0}}
                    """));
        }
    }

    private void enqueueFailureResponses(int count) {
        for (int index = 0; index < count; index++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        }
    }

    private long percentile(List<Long> values, double percentile) {
        int index = (int) Math.ceil(percentile * values.size()) - 1;
        return values.get(index);
    }

    private record WorkerMeasurement(
        int workerCount,
        int requestCount,
        long p50Millis,
        long p95Millis,
        long maxMillis
    ) {
    }

    private record RetryMeasurement(
        int retryCount,
        int providerCallCount,
        boolean ready,
        long elapsedMillis,
        long timeoutUpperBoundSeconds
    ) {
    }
}
