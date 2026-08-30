package akuma.whiplash.domains.alarm.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.AlarmOccurrenceFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.ad.domain.constant.AdPurpose;
import akuma.whiplash.domains.ad.domain.constant.AdSessionStatus;
import akuma.whiplash.domains.ad.domain.service.AdSessionService;
import akuma.whiplash.domains.ad.persistence.entity.AdSessionEntity;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmAdActionRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmOffAdSessionCreateRequest;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.*;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.*;
import akuma.whiplash.global.util.date.TimeProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlarmCommandService Unit Test")
class AlarmCommandServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 2, 14, 30);
    @Mock private AlarmRepository alarmRepository;
    @Mock private AlarmOccurrenceRepository alarmOccurrenceRepository;
    @Mock private AlarmRingingLogRepository alarmRingingLogRepository;
    @Mock private AlarmDeactivationLogRepository alarmDeactivationLogRepository;
    @Mock private AlarmDeleteLogRepository alarmDeleteLogRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberDeviceRepository memberDeviceRepository;
    @Mock private AdSessionService adSessionService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private TimeProvider timeProvider;
    @Mock private AlarmLocationCacheService alarmLocationCacheService;
    @InjectMocks private AlarmCommandServiceImpl alarmCommandService;

    @BeforeEach
    void setUp() {
        given(timeProvider.now()).willReturn(NOW);
    }

    @Nested
    @DisplayName("광고 세션")
    class AdSessionTest {
        @Test
        @DisplayName("알람 끄기 세션은 회차와 인증 기기에 결합해 발급한다")
        void createsOffSession() {
            MemberEntity member = MemberFixture.MEMBER_9.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_09.toMockEntity(member);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_01.toMockEntity(
                alarm, 1L, NOW.plusHours(1), OccurrenceStatus.SCHEDULED);
            AdSessionEntity session = AdSessionEntity.builder().adSessionId("session").expiresAt(NOW.plusMinutes(10)).build();
            given(alarmRepository.findByIdWithMemberForUpdate(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findByIdAndAlarmId(occurrence.getId(), alarm.getId())).willReturn(Optional.of(occurrence));
            given(adSessionService.createSession(member, alarm, occurrence, "device", AdPurpose.STOP_ALARM, NOW.plusMinutes(10))).willReturn(session);

            alarmCommandService.createOffAdSession(member.getId(), "device", alarm.getId(), new AlarmOffAdSessionCreateRequest(occurrence.getId()));

            verify(adSessionService).createSession(member, alarm, occurrence, "device", AdPurpose.STOP_ALARM, NOW.plusMinutes(10));
        }

        @Test
        @DisplayName("검증된 광고 세션으로 회차를 WATCH_AD 상태로 전환하고 소비한다")
        void deactivatesByAd() {
            MemberEntity member = MemberFixture.MEMBER_9.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_09.toMockEntity(member);
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceFixture.ALARM_OCCURRENCE_01.toMockEntity(
                alarm, 1L, NOW.plusHours(1), OccurrenceStatus.SCHEDULED);
            AdSessionEntity session = AdSessionEntity.builder().adSessionId("session").member(member).alarm(alarm).alarmOccurrence(occurrence)
                .deviceId("device").purpose(AdPurpose.STOP_ALARM).status(AdSessionStatus.VERIFIED).expiresAt(NOW.plusMinutes(10)).build();
            given(alarmRepository.findByIdWithMemberForUpdate(alarm.getId())).willReturn(Optional.of(alarm));
            given(adSessionService.getSessionTarget("session")).willReturn(session);
            given(alarmOccurrenceRepository.findByIdAndAlarmId(occurrence.getId(), alarm.getId())).willReturn(Optional.of(occurrence));
            given(adSessionService.getVerifiedSessionForConsume("session", member.getId(), alarm.getId(), "device", AdPurpose.STOP_ALARM, occurrence.getId())).willReturn(session);
            given(alarmOccurrenceRepository.findNextScheduledByAlarmIds(List.of(alarm.getId()), OccurrenceStatus.SCHEDULED, NOW)).willReturn(List.of());

            alarmCommandService.deactivateByAd(member.getId(), "device", alarm.getId(), new AlarmAdActionRequest("session"));

            assertThat(occurrence.getStatus()).isEqualTo(OccurrenceStatus.WATCH_AD);
            assertThat(session.getStatus()).isEqualTo(AdSessionStatus.CONSUMED);
            verify(alarmDeactivationLogRepository).save(any());
        }
    }
}
