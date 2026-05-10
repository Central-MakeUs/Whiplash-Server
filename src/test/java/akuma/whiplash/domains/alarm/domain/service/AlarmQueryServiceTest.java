package akuma.whiplash.domains.alarm.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmDeleteMethodResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmSyncResponse;
import akuma.whiplash.domains.alarm.application.dto.response.GetAlarmsResponse;
import akuma.whiplash.domains.alarm.domain.constant.AlarmDeleteMethod;
import akuma.whiplash.domains.alarm.domain.constant.AlarmStatus;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.domain.constant.SoundType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.exception.AlarmErrorCode;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.util.date.TimeProvider;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    @Mock private TimeProvider timeProvider;

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

            // when
            var thrown = assertThatThrownBy(() -> alarmQueryService.getAlarms(memberId));

            // then
            thrown
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getSyncAlarms - 알람 전체 동기화 조회")
    class GetSyncAlarmsTest {

        @Test
        @DisplayName("성공: 삭제되지 않은 알람과 다음 예정 회차를 반환한다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_11.toMockEntity();
            AlarmEntity activeAlarm = AlarmFixture.ALARM_11.toMockEntity();
            AlarmEntity inactiveAlarm = AlarmEntity.builder()
                .id(12L)
                .member(member)
                .alarmPurpose("휴식")
                .time(LocalTime.of(9, 0))
                .repeatDays(List.of(Weekday.MONDAY))
                .soundType(SoundType.ONE)
                .latitude(37.0)
                .longitude(127.0)
                .address("서울")
                .revision(3)
                .status(AlarmStatus.INACTIVE)
                .build();
            AlarmOccurrenceEntity nextOccurrence = AlarmOccurrenceEntity.builder()
                .id(100L)
                .alarm(activeAlarm)
                .occurrenceDate(LocalDate.now().plusDays(1))
                .occurrenceTime(activeAlarm.getTime())
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .status(OccurrenceStatus.SCHEDULED)
                .alarmRinging(false)
                .ringingCount(0)
                .reminderSent(false)
                .build();

            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
            given(alarmRepository.findAllByMemberIdAndStatusNot(member.getId(), AlarmStatus.DELETED))
                .willReturn(List.of(activeAlarm, inactiveAlarm));
            given(alarmOccurrenceRepository.findNextScheduledByAlarmIds(anyList(), any(), any(LocalDateTime.class)))
                .willReturn(List.of(nextOccurrence));

            // when
            AlarmSyncResponse result = alarmQueryService.getSyncAlarms(member.getId());

            // then
            assertThat(result.serverTime()).isNotNull();
            assertThat(result.alarms()).hasSize(2);
            assertThat(result.alarms().get(0).alarmId()).isEqualTo(activeAlarm.getId());
            assertThat(result.alarms().get(0).status()).isEqualTo("활성화");
            assertThat(result.alarms().get(0).nextOccurrence().occurrenceId()).isEqualTo(nextOccurrence.getId());
            assertThat(result.alarms().get(1).status()).isEqualTo("비활성화");
            assertThat(result.alarms().get(1).nextOccurrence()).isNull();
        }

        @Nested
        @DisplayName("빈 알람")
        class EmptyAlarmsTest {

            @Test
            @DisplayName("성공: 알람이 없으면 회차 조회 없이 빈 목록을 반환한다")
            void success() {
                // given
                MemberEntity member = MemberFixture.MEMBER_11.toMockEntity();
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(alarmRepository.findAllByMemberIdAndStatusNot(member.getId(), AlarmStatus.DELETED))
                    .willReturn(List.of());

                // when
                AlarmSyncResponse result = alarmQueryService.getSyncAlarms(member.getId());

                // then
                assertThat(result.serverTime()).isNotNull();
                assertThat(result.alarms()).isEmpty();
                then(alarmOccurrenceRepository).should(never())
                    .findNextScheduledByAlarmIds(anyList(), any(), any(LocalDateTime.class));
            }
        }

        @Test
        @DisplayName("실패: 회원이 존재하지 않으면 예외를 던진다")
        void fail_memberNotFound() {
            // given
            long memberId = 999L;
            given(memberRepository.findById(memberId)).willReturn(Optional.empty());

            // when
            var thrown = assertThatThrownBy(() -> alarmQueryService.getSyncAlarms(memberId));

            // then
            thrown
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getAlarmDeleteMethod - 알람 삭제 방법 조회")
    class GetAlarmDeleteMethodTest {

        private static final LocalDate TODAY = LocalDate.of(2026, 5, 4);

        @Nested
        @DisplayName("오늘 회차가 없는 경우")
        class NoOccurrenceTodayTest {

            @Test
            @DisplayName("성공: 광고 시청으로 삭제할 수 있다")
            void success() {
                // given
                MemberEntity member = MemberFixture.MEMBER_11.toMockEntity();
                AlarmEntity alarm = AlarmFixture.ALARM_11.toMockEntity(member);
                given(alarmRepository.findByIdWithMember(alarm.getId())).willReturn(Optional.of(alarm));
                given(timeProvider.today()).willReturn(TODAY);
                given(alarmOccurrenceRepository.findStatusByAlarmIdAndDate(alarm.getId(), TODAY))
                    .willReturn(Optional.empty());

                // when
                AlarmDeleteMethodResponse result = alarmQueryService.getAlarmDeleteMethod(member.getId(), alarm.getId());

                // then
                assertThat(result.deleteMethod()).isEqualTo(AlarmDeleteMethod.AD.name());
            }
        }

        @Nested
        @DisplayName("오늘 회차가 도착 인증 완료 상태인 경우")
        class CheckinCompletedTest {

            @Test
            @DisplayName("성공: 광고 시청으로 삭제할 수 있다")
            void success() {
                // given
                MemberEntity member = MemberFixture.MEMBER_11.toMockEntity();
                AlarmEntity alarm = AlarmFixture.ALARM_11.toMockEntity(member);
                given(alarmRepository.findByIdWithMember(alarm.getId())).willReturn(Optional.of(alarm));
                given(timeProvider.today()).willReturn(TODAY);
                given(alarmOccurrenceRepository.findStatusByAlarmIdAndDate(alarm.getId(), TODAY))
                    .willReturn(Optional.of(OccurrenceStatus.CHECKIN));

                // when
                AlarmDeleteMethodResponse result = alarmQueryService.getAlarmDeleteMethod(member.getId(), alarm.getId());

                // then
                assertThat(result.deleteMethod()).isEqualTo(AlarmDeleteMethod.AD.name());
            }
        }

        @Nested
        @DisplayName("오늘 회차가 결제 완료 상태인 경우")
        class PaymentCompletedTest {

            @Test
            @DisplayName("성공: 광고 시청으로 삭제할 수 있다")
            void success() {
                // given
                MemberEntity member = MemberFixture.MEMBER_11.toMockEntity();
                AlarmEntity alarm = AlarmFixture.ALARM_11.toMockEntity(member);
                given(alarmRepository.findByIdWithMember(alarm.getId())).willReturn(Optional.of(alarm));
                given(timeProvider.today()).willReturn(TODAY);
                given(alarmOccurrenceRepository.findStatusByAlarmIdAndDate(alarm.getId(), TODAY))
                    .willReturn(Optional.of(OccurrenceStatus.PAYMENT));

                // when
                AlarmDeleteMethodResponse result = alarmQueryService.getAlarmDeleteMethod(member.getId(), alarm.getId());

                // then
                assertThat(result.deleteMethod()).isEqualTo(AlarmDeleteMethod.AD.name());
            }
        }

        @Nested
        @DisplayName("오늘 회차가 예정 상태인 경우")
        class ScheduledTest {

            @Test
            @DisplayName("성공: 결제로 삭제할 수 있다")
            void success() {
                // given
                MemberEntity member = MemberFixture.MEMBER_11.toMockEntity();
                AlarmEntity alarm = AlarmFixture.ALARM_11.toMockEntity(member);
                given(alarmRepository.findByIdWithMember(alarm.getId())).willReturn(Optional.of(alarm));
                given(timeProvider.today()).willReturn(TODAY);
                given(alarmOccurrenceRepository.findStatusByAlarmIdAndDate(alarm.getId(), TODAY))
                    .willReturn(Optional.of(OccurrenceStatus.SCHEDULED));

                // when
                AlarmDeleteMethodResponse result = alarmQueryService.getAlarmDeleteMethod(member.getId(), alarm.getId());

                // then
                assertThat(result.deleteMethod()).isEqualTo(AlarmDeleteMethod.PAYMENT.name());
            }
        }

        @Nested
        @DisplayName("오늘 회차가 울림 상태인 경우")
        class RingingTest {

            @Test
            @DisplayName("성공: 결제로 삭제할 수 있다")
            void success() {
                // given
                MemberEntity member = MemberFixture.MEMBER_11.toMockEntity();
                AlarmEntity alarm = AlarmFixture.ALARM_11.toMockEntity(member);
                given(alarmRepository.findByIdWithMember(alarm.getId())).willReturn(Optional.of(alarm));
                given(timeProvider.today()).willReturn(TODAY);
                given(alarmOccurrenceRepository.findStatusByAlarmIdAndDate(alarm.getId(), TODAY))
                    .willReturn(Optional.of(OccurrenceStatus.RINGING));

                // when
                AlarmDeleteMethodResponse result = alarmQueryService.getAlarmDeleteMethod(member.getId(), alarm.getId());

                // then
                assertThat(result.deleteMethod()).isEqualTo(AlarmDeleteMethod.PAYMENT.name());
            }
        }

        @Test
        @DisplayName("실패: 알람이 존재하지 않으면 예외를 던진다")
        void fail_alarmNotFound() {
            // given
            long alarmId = 999L;
            given(alarmRepository.findByIdWithMember(alarmId)).willReturn(Optional.empty());

            // when
            var thrown = assertThatThrownBy(() -> alarmQueryService.getAlarmDeleteMethod(1L, alarmId));

            // then
            thrown
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(AlarmErrorCode.ALARM_NOT_FOUND));
        }

        @Test
        @DisplayName("실패: 알람 소유자가 아니면 예외를 던진다")
        void fail_permissionDenied() {
            // given
            MemberEntity owner = MemberFixture.MEMBER_11.toMockEntity();
            MemberEntity other = MemberFixture.MEMBER_12.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_11.toMockEntity(owner);
            given(alarmRepository.findByIdWithMember(alarm.getId())).willReturn(Optional.of(alarm));

            // when
            var thrown = assertThatThrownBy(() -> alarmQueryService.getAlarmDeleteMethod(other.getId(), alarm.getId()));

            // then
            thrown
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(AuthErrorCode.PERMISSION_DENIED));
        }
    }
}
