package akuma.whiplash.domains.alarm.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.dto.response.GetAlarmsResponse;
import akuma.whiplash.domains.alarm.domain.constant.AlarmStatus;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.exception.ApplicationException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("AlarmQueryService Unit Test")
@ExtendWith(MockitoExtension.class)
class AlarmQueryServiceTest {

    @Mock private AlarmRepository alarmRepository;
    @Mock private AlarmOccurrenceRepository alarmOccurrenceRepository;
    @Mock private MemberRepository memberRepository;

    @InjectMocks private AlarmQueryServiceImpl alarmQueryService;

    @Nested
    @DisplayName("getAlarms - 알람 목록 조회")
    class GetAlarmsTest {

        @Test
        @DisplayName("성공: result.alarms 래퍼로 알람 목록을 반환한다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_11.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_11.toMockEntity();
            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
            given(alarmRepository.findAllByMemberIdAndStatusNot(member.getId(), AlarmStatus.DELETED))
                .willReturn(List.of(alarm));
            given(alarmOccurrenceRepository.findLatestProcessedByAlarmIds(anyList(), anyList()))
                .willReturn(List.of());
            given(alarmOccurrenceRepository.findByAlarmIdsAndOccurrenceDates(anyList(), anyList()))
                .willReturn(List.of());

            // when
            GetAlarmsResponse result = alarmQueryService.getAlarms(member.getId());

            // then
            assertThat(result.alarms()).hasSize(1);
            assertThat(result.alarms().get(0).alarmId()).isEqualTo(alarm.getId());
        }

        @Test
        @DisplayName("성공: alarm.status=INACTIVE이면 status=비활성화이다")
        void success_statusDeactivatedWhenAlarmInactive() {
            // given
            MemberEntity member = MemberFixture.MEMBER_11.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_11.toMockEntity();
            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
            given(alarmRepository.findAllByMemberIdAndStatusNot(member.getId(), AlarmStatus.DELETED))
                .willReturn(List.of(alarm));
            given(alarmOccurrenceRepository.findLatestProcessedByAlarmIds(anyList(), anyList()))
                .willReturn(List.of());
            given(alarmOccurrenceRepository.findByAlarmIdsAndOccurrenceDates(anyList(), anyList()))
                .willReturn(List.of());

            // when
            GetAlarmsResponse result = alarmQueryService.getAlarms(member.getId());

            // then
            assertThat(result.alarms()).hasSize(1);
        }

        @Test
        @DisplayName("성공: 현재 회차가 CHECKIN이면 nextOccurrence가 다음 텀으로 밀린다")
        void success_statusActiveAndMoveNextWhenCheckin() {
            // given
            MemberEntity member = MemberFixture.MEMBER_11.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_11.toMockEntity();
            AlarmOccurrenceEntity processed = AlarmOccurrenceEntity.builder()
                .alarm(alarm)
                .occurrenceDate(LocalDate.now())
                .occurrenceTime(alarm.getTime())
                .scheduledAt(LocalDate.now().atTime(alarm.getTime()))
                .status(OccurrenceStatus.CHECKIN)
                .alarmRinging(false)
                .ringingCount(0)
                .reminderSent(false)
                .build();

            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
            given(alarmRepository.findAllByMemberIdAndStatusNot(member.getId(), AlarmStatus.DELETED))
                .willReturn(List.of(alarm));
            given(alarmOccurrenceRepository.findLatestProcessedByAlarmIds(anyList(), anyList()))
                .willReturn(List.of(processed));
            given(alarmOccurrenceRepository.findByAlarmIdsAndOccurrenceDates(anyList(), anyList()))
                .willReturn(List.of());

            // when
            GetAlarmsResponse result = alarmQueryService.getAlarms(member.getId());

            // then
            assertThat(result.alarms()).hasSize(1);
            assertThat(result.alarms().get(0).status()).isEqualTo("활성화");
            assertThat(result.alarms().get(0).nextOccurrence().scheduledDate())
                .isAfter(LocalDate.now());
        }

        @Test
        @DisplayName("성공: DELETED 알람은 목록에서 제외한다")
        void success_excludeDeletedAlarms() {
            // given
            MemberEntity member = MemberFixture.MEMBER_11.toMockEntity();
            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
            given(alarmRepository.findAllByMemberIdAndStatusNot(member.getId(), AlarmStatus.DELETED))
                .willReturn(List.of());

            // when
            GetAlarmsResponse result = alarmQueryService.getAlarms(member.getId());

            // then
            assertThat(result.alarms()).isEmpty();
        }

        @Test
        @DisplayName("실패: 회원이 존재하지 않으면 예외를 던진다")
        void fail_memberNotFound() {
            // given
            long memberId = 999L;
            given(memberRepository.findById(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alarmQueryService.getAlarms(memberId))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }
}
