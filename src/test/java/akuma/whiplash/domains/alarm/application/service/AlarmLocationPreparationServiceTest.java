package akuma.whiplash.domains.alarm.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.AlarmOccurrenceFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.dto.response.LocationPreparationResponse;
import akuma.whiplash.domains.alarm.application.dto.response.LocationPreparationState;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.domain.service.AlarmLocationCacheService;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.place.exception.PlaceErrorCode;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.util.date.TimeProvider;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlarmLocationPreparationService Unit Test")
class AlarmLocationPreparationServiceTest {

    private static final Long MEMBER_ID = MemberFixture.MEMBER_10.getId();
    private static final Long ALARM_ID = AlarmFixture.ALARM_10.getId();
    private static final Long OCCURRENCE_ID = 501L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 2, 14, 30);

    @Mock
    private AlarmRepository alarmRepository;
    @Mock
    private AlarmOccurrenceRepository alarmOccurrenceRepository;
    @Mock
    private AlarmLocationCacheService alarmLocationCacheService;
    @Mock
    private TimeProvider timeProvider;
    @Mock
    private TaskExecutor locationPreparationExecutor;
    @Mock
    private TaskScheduler taskScheduler;

    private AlarmLocationPreparationService service;
    private AlarmEntity alarm;
    private AlarmOccurrenceEntity occurrence;

    @BeforeEach
    void setUp() {
        service = new AlarmLocationPreparationService(
            alarmRepository,
            alarmOccurrenceRepository,
            alarmLocationCacheService,
            timeProvider,
            locationPreparationExecutor,
            taskScheduler,
            new SimpleMeterRegistry()
        );
        alarm = AlarmFixture.ALARM_10.toMockEntity(MemberFixture.MEMBER_10.toMockEntity());
        alarm.updateGooglePlaceLocation(
            "google-place-id",
            alarm.getAddress(),
            alarm.getLatitude(),
            alarm.getLongitude(),
            NOW.minusDays(29)
        );
        occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_02.toMockEntity(
            alarm,
            OCCURRENCE_ID,
            NOW.plusHours(1),
            OccurrenceStatus.SCHEDULED
        );
        given(timeProvider.now()).willReturn(NOW);
        lenient().when(timeProvider.instant()).thenReturn(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant());
        given(alarmRepository.findByIdWithMember(ALARM_ID)).willReturn(Optional.of(alarm));
        given(alarmOccurrenceRepository.findByIdAndAlarmIdForLocationPreparation(OCCURRENCE_ID, ALARM_ID))
            .willReturn(Optional.of(occurrence));
    }

    @Nested
    @DisplayName("getLocationPreparation - 인증 화면 위치 준비")
    class GetLocationPreparationTest {

        @Test
        @DisplayName("성공: 유효한 cache면 worker 없이 READY를 반환한다")
        void success_readyCache() {
            // given
            given(alarmLocationCacheService.hasValidGoogleLocationCache(alarm, NOW)).willReturn(true);

            // when
            LocationPreparationResponse response = service.getLocationPreparation(MEMBER_ID, ALARM_ID, OCCURRENCE_ID);

            // then
            assertThat(response.state()).isEqualTo(LocationPreparationState.READY);
            assertThat(response.canCheckin()).isTrue();
            assertThat(response.targetLocation().latitude()).isEqualTo(alarm.getLatitude());
            verifyNoInteractions(locationPreparationExecutor);
        }

        @Test
        @DisplayName("성공: 만료 cache면 요청 thread를 기다리지 않고 PREPARING을 반환한다")
        void success_returnsPreparingBeforeGoogleRefreshCompletes() {
            // given
            AtomicReference<Runnable> task = captureWorkerTask();
            given(alarmLocationCacheService.hasValidGoogleLocationCache(alarm, NOW)).willReturn(false, false, true);
            given(alarmRepository.findById(ALARM_ID)).willReturn(Optional.of(alarm));
            doNothing().when(alarmLocationCacheService).modifyGoogleLocationCache(ALARM_ID);

            // when
            LocationPreparationResponse initial = service.getLocationPreparation(MEMBER_ID, ALARM_ID, OCCURRENCE_ID);
            task.get().run();
            LocationPreparationResponse completed = service.getLocationPreparation(MEMBER_ID, ALARM_ID, OCCURRENCE_ID);

            // then
            assertThat(initial.state()).isEqualTo(LocationPreparationState.PREPARING);
            assertThat(completed.state()).isEqualTo(LocationPreparationState.READY);
            verify(alarmLocationCacheService).modifyGoogleLocationCache(ALARM_ID);
        }

        @Test
        @DisplayName("성공: 같은 알람의 동시 요청은 하나의 refresh 작업으로 병합한다")
        void success_coalescesSameAlarmRefresh() {
            // given
            captureWorkerTask();
            given(alarmLocationCacheService.hasValidGoogleLocationCache(alarm, NOW)).willReturn(false);

            // when
            LocationPreparationResponse first = service.getLocationPreparation(MEMBER_ID, ALARM_ID, OCCURRENCE_ID);
            LocationPreparationResponse second = service.getLocationPreparation(MEMBER_ID, ALARM_ID, OCCURRENCE_ID);

            // then
            assertThat(first.state()).isEqualTo(LocationPreparationState.PREPARING);
            assertThat(second.state()).isEqualTo(LocationPreparationState.PREPARING);
            verify(locationPreparationExecutor).execute(org.mockito.ArgumentMatchers.any(Runnable.class));
        }

        @Test
        @DisplayName("성공: retryable provider 실패는 한 번만 자동 재시도한다")
        void success_retriesOnce() {
            // given
            AtomicReference<Runnable> task = captureWorkerTask();
            given(alarmLocationCacheService.hasValidGoogleLocationCache(alarm, NOW)).willReturn(false, false, true);
            given(alarmRepository.findById(ALARM_ID)).willReturn(Optional.of(alarm));
            doThrow(ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR))
                .doNothing()
                .when(alarmLocationCacheService).modifyGoogleLocationCache(ALARM_ID);

            // when
            service.getLocationPreparation(MEMBER_ID, ALARM_ID, OCCURRENCE_ID);
            task.get().run();
            LocationPreparationResponse response = service.getLocationPreparation(MEMBER_ID, ALARM_ID, OCCURRENCE_ID);

            // then
            assertThat(response.state()).isEqualTo(LocationPreparationState.READY);
            verify(alarmLocationCacheService, org.mockito.Mockito.times(2)).modifyGoogleLocationCache(ALARM_ID);
        }

        private AtomicReference<Runnable> captureWorkerTask() {
            AtomicReference<Runnable> task = new AtomicReference<>();
            doAnswer(invocation -> {
                task.set(invocation.getArgument(0));
                return null;
            }).when(locationPreparationExecutor).execute(org.mockito.ArgumentMatchers.any(Runnable.class));
            return task;
        }
    }
}
