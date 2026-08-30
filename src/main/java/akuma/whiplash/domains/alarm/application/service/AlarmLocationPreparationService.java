package akuma.whiplash.domains.alarm.application.service;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.ALARM_NOT_FOUND;
import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.ALARM_OCCURRENCE_NOT_FOUND;
import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.ALREADY_DEACTIVATED;
import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.CHECKIN_NOT_YET_AVAILABLE;
import static akuma.whiplash.domains.auth.exception.AuthErrorCode.PERMISSION_DENIED;

import akuma.whiplash.domains.alarm.application.dto.response.LocationPreparationResponse;
import akuma.whiplash.domains.alarm.application.dto.response.LocationPreparationState;
import akuma.whiplash.domains.alarm.domain.constant.LocationSource;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.domain.service.AlarmLocationCacheService;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.place.exception.PlaceErrorCode;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.log.NoMethodLog;
import akuma.whiplash.global.util.date.TimeProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@NoMethodLog
public class AlarmLocationPreparationService {

    private static final Duration UX_DELAY = Duration.ofSeconds(5);
    private static final Duration HARD_DEADLINE = Duration.ofSeconds(30);
    private static final Duration PROVIDER_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_ATTEMPT_COUNT = 2;

    private final AlarmRepository alarmRepository;
    private final AlarmOccurrenceRepository alarmOccurrenceRepository;
    private final AlarmLocationCacheService alarmLocationCacheService;
    private final TimeProvider timeProvider;
    @Qualifier("locationPreparationExecutor")
    private final TaskExecutor locationPreparationExecutor;
    private final TaskScheduler taskScheduler;
    private final MeterRegistry meterRegistry;

    private final ConcurrentMap<Long, RefreshJob> refreshJobs = new ConcurrentHashMap<>();

    public LocationPreparationResponse getLocationPreparation(Long memberId, Long alarmId, Long occurrenceId) {
        AlarmEntity alarm = alarmRepository.findByIdWithMember(alarmId)
            .orElseThrow(() -> ApplicationException.from(ALARM_NOT_FOUND));
        validateOwner(memberId, alarm);
        AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository
            .findByIdAndAlarmIdForLocationPreparation(occurrenceId, alarmId)
            .orElseThrow(() -> ApplicationException.from(ALARM_OCCURRENCE_NOT_FOUND));
        LocalDateTime now = timeProvider.now();
        validateCheckinAvailable(occurrence, now);

        if (hasReadyLocation(alarm, now)) {
            return LocationPreparationResponse.ready(alarm.getLatitude(), alarm.getLongitude(), alarm.getAddress());
        }
        if (alarm.getLocationSource() != LocationSource.GOOGLE_PLACE || !alarm.hasGooglePlaceId()) {
            return LocationPreparationResponse.terminal(LocationPreparationState.UNAVAILABLE);
        }

        RefreshJob candidate = new RefreshJob(timeProvider.instant());
        RefreshJob existing = refreshJobs.putIfAbsent(alarmId, candidate);
        if (existing != null) {
            meterRegistry.counter("alarm.location_prepare.coalesced").increment();
            return mapJobToResponse(existing);
        }

        meterRegistry.counter("alarm.location_prepare.start").increment();
        try {
            locationPreparationExecutor.execute(() -> refreshGoogleLocation(alarmId, candidate));
        } catch (TaskRejectedException exception) {
            candidate.reject();
            meterRegistry.counter("alarm.location_prepare.rejected").increment();
            refreshJobs.remove(alarmId, candidate);
        }
        return mapJobToResponse(candidate);
    }

    private void refreshGoogleLocation(Long alarmId, RefreshJob job) {
        try {
            if (hasReachedDeadline(job)) {
                job.exhaust();
                return;
            }
            job.run();
            AlarmEntity alarm = alarmRepository.findById(alarmId).orElse(null);
            if (alarm == null || alarm.getLocationSource() != LocationSource.GOOGLE_PLACE || !alarm.hasGooglePlaceId()) {
                job.unavailable();
                return;
            }
            if (alarmLocationCacheService.hasValidGoogleLocationCache(alarm, timeProvider.now())) {
                job.succeed();
                return;
            }

            attemptRefresh(alarmId, job);
        } finally {
            recordOutcome(job);
            scheduleRemoval(alarmId, job);
        }
    }

    private void attemptRefresh(Long alarmId, RefreshJob job) {
        while (!hasReachedDeadline(job) && job.getAttemptCount() < MAX_ATTEMPT_COUNT) {
            int attempt = job.beginAttempt();
            meterRegistry.counter("alarm.location_prepare.provider_call", "attempt", String.valueOf(attempt)).increment();
            try {
                alarmLocationCacheService.modifyGoogleLocationCache(alarmId);
                job.succeed();
                return;
            } catch (ApplicationException exception) {
                if (!isRetryable(exception)
                    || job.getAttemptCount() >= MAX_ATTEMPT_COUNT
                    || !hasTimeForNextAttempt(job)) {
                    job.exhaust();
                    return;
                }
                job.retry();
            } catch (RuntimeException exception) {
                job.exhaust();
                return;
            }
        }
        job.exhaust();
    }

    private boolean hasReadyLocation(AlarmEntity alarm, LocalDateTime now) {
        if (alarm.getLocationSource() == LocationSource.GOOGLE_PLACE) {
            return alarmLocationCacheService.hasValidGoogleLocationCache(alarm, now);
        }
        return alarm.getLatitude() != null && alarm.getLongitude() != null;
    }

    private void validateOwner(Long memberId, AlarmEntity alarm) {
        if (!alarm.getMember().getId().equals(memberId)) {
            throw ApplicationException.from(PERMISSION_DENIED);
        }
    }

    private void validateCheckinAvailable(AlarmOccurrenceEntity occurrence, LocalDateTime now) {
        if (occurrence.getStatus() != OccurrenceStatus.SCHEDULED && occurrence.getStatus() != OccurrenceStatus.RINGING) {
            throw ApplicationException.from(ALREADY_DEACTIVATED);
        }
        if (now.isBefore(occurrence.getScheduledAt().minusHours(3))) {
            throw ApplicationException.from(CHECKIN_NOT_YET_AVAILABLE);
        }
    }

    private boolean isRetryable(ApplicationException exception) {
        return exception.getCode() == PlaceErrorCode.PROVIDER_TIMEOUT
            || exception.getCode() == PlaceErrorCode.PROVIDER_ERROR;
    }

    private boolean hasReachedDeadline(RefreshJob job) {
        return !timeProvider.instant().isBefore(job.hardDeadline());
    }

    private boolean hasTimeForNextAttempt(RefreshJob job) {
        return !timeProvider.instant().plus(PROVIDER_TIMEOUT).isAfter(job.hardDeadline());
    }

    private LocationPreparationResponse mapJobToResponse(RefreshJob job) {
        return switch (job.state()) {
            case RETRYING -> LocationPreparationResponse.waiting(LocationPreparationState.RETRYING);
            case EXHAUSTED -> LocationPreparationResponse.terminal(LocationPreparationState.EXHAUSTED);
            case UNAVAILABLE -> LocationPreparationResponse.terminal(LocationPreparationState.UNAVAILABLE);
            case REJECTED -> LocationPreparationResponse.terminal(LocationPreparationState.BUSY);
            case SUCCEEDED, QUEUED, RUNNING -> job.isDelayed(timeProvider.instant())
                ? LocationPreparationResponse.waiting(LocationPreparationState.DELAYED)
                : LocationPreparationResponse.waiting(LocationPreparationState.PREPARING);
        };
    }

    private void recordOutcome(RefreshJob job) {
        String result = job.state().name().toLowerCase();
        meterRegistry.counter("alarm.location_prepare.outcome", "result", result).increment();
        Timer.builder("alarm.location_prepare.duration")
            .tag("result", result)
            .register(meterRegistry)
            .record(Duration.between(job.startedAt(), timeProvider.instant()));
    }

    private void scheduleRemoval(Long alarmId, RefreshJob job) {
        taskScheduler.schedule(() -> refreshJobs.remove(alarmId, job), job.hardDeadline());
    }

    private enum RefreshState {
        QUEUED,
        RUNNING,
        RETRYING,
        SUCCEEDED,
        EXHAUSTED,
        UNAVAILABLE,
        REJECTED
    }

    private static class RefreshJob {

        private final Instant startedAt;
        private final Instant hardDeadline;
        private final AtomicInteger attemptCount = new AtomicInteger();
        private volatile RefreshState state = RefreshState.QUEUED;

        private RefreshJob(Instant startedAt) {
            this.startedAt = startedAt;
            this.hardDeadline = startedAt.plus(HARD_DEADLINE);
        }

        private int beginAttempt() {
            return attemptCount.incrementAndGet();
        }

        private int getAttemptCount() {
            return attemptCount.get();
        }

        private Instant hardDeadline() {
            return hardDeadline;
        }

        private Instant startedAt() {
            return startedAt;
        }

        private RefreshState state() {
            return state;
        }

        private boolean isDelayed(Instant now) {
            return !now.isBefore(startedAt.plus(UX_DELAY));
        }

        private void run() {
            state = RefreshState.RUNNING;
        }

        private void retry() {
            state = RefreshState.RETRYING;
        }

        private void succeed() {
            state = RefreshState.SUCCEEDED;
        }

        private void exhaust() {
            state = RefreshState.EXHAUSTED;
        }

        private void unavailable() {
            state = RefreshState.UNAVAILABLE;
        }

        private void reject() {
            state = RefreshState.REJECTED;
        }
    }
}
