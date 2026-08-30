package akuma.whiplash.domains.alarm.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmSyncResponse;
import akuma.whiplash.domains.alarm.domain.constant.AlarmStatus;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.util.date.TimeProvider;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlarmQueryService Unit Test")
class AlarmQueryServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 2, 14, 30);
    @Mock private AlarmRepository alarmRepository;
    @Mock private AlarmOccurrenceRepository alarmOccurrenceRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberDeviceRepository memberDeviceRepository;
    @Mock private TimeProvider timeProvider;
    @InjectMocks private AlarmQueryServiceImpl alarmQueryService;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(timeProvider.now()).thenReturn(NOW);
        Mockito.lenient().when(timeProvider.now(any(ZoneId.class))).thenReturn(NOW);
    }

    @Test
    @DisplayName("알람 동기화는 다음 SCHEDULED 회차만 포함한다")
    void getsSyncAlarms() {
        MemberEntity member = MemberFixture.MEMBER_11.toMockEntity();
        AlarmEntity alarm = AlarmFixture.ALARM_11.toMockEntity(member);
        AlarmOccurrenceEntity occurrence = AlarmOccurrenceEntity.builder().id(3L).alarm(alarm)
            .occurrenceDate(NOW.toLocalDate()).occurrenceTime(LocalTime.of(15, 30))
            .scheduledAt(NOW.plusHours(1)).status(OccurrenceStatus.SCHEDULED).build();
        given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
        given(alarmRepository.findAllByMemberIdAndStatusNot(member.getId(), AlarmStatus.DELETED)).willReturn(List.of(alarm));
        given(alarmOccurrenceRepository.findNextScheduledByAlarmIds(anyList(), eq(OccurrenceStatus.SCHEDULED), eq(NOW))).willReturn(List.of(occurrence));

        AlarmSyncResponse response = alarmQueryService.getSyncAlarms(member.getId(), "device");

        assertThat(response.alarms()).singleElement().satisfies(item -> assertThat(item.nextOccurrence().occurrenceId()).isEqualTo(3L));
    }

    @Test
    @DisplayName("사전 알림 대상은 SCHEDULED 회차로 조회한다")
    void getsPreNotificationTargets() {
        OccurrencePushInfo target = new OccurrencePushInfo(1L, 2L, 3L);
        given(alarmOccurrenceRepository.findPreNotificationTargetsByScheduledAtBetween(NOW, NOW.plusMinutes(1), OccurrenceStatus.SCHEDULED)).willReturn(List.of(target));

        assertThat(alarmQueryService.getPreNotificationTargets(NOW, NOW.plusMinutes(1))).containsExactly(target);
    }
}
