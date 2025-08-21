package akuma.whiplash.domains.alarm.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOffLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRingingLogRepository;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.exception.ApplicationException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlarmCommandService Unit Test")
class AlarmCommandServiceTest {

    @Mock
    private AlarmRepository alarmRepository;
    @Mock
    private AlarmOccurrenceRepository alarmOccurrenceRepository;
    @Mock
    private AlarmOffLogRepository alarmOffLogRepository;
    @Mock
    private AlarmRingingLogRepository alarmRingingLogRepository;
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AlarmCommandServiceImpl alarmCommandService;

    @BeforeEach
    void setUp() {
        alarmCommandService = new AlarmCommandServiceImpl(
            alarmRepository, alarmOccurrenceRepository, alarmOffLogRepository, alarmRingingLogRepository, memberRepository);
    }

    @Nested
    @DisplayName("createAlarm - 알람 등록")
    class CreateAlarmTest {

        @Test
        @DisplayName("회원이 알람 등록을 요청하면 알람이 저장된다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_5.toMockEntity();
            AlarmFixture fixture = AlarmFixture.ALARM_05;
            AlarmRegisterRequest request = new AlarmRegisterRequest(
                fixture.getAddress(),
                fixture.getLatitude(),
                fixture.getLongitude(),
                fixture.getAlarmPurpose(),
                fixture.getTime(),
                fixture.getRepeatDays().stream().map(Weekday::getDescription).toList(),
                fixture.getSoundType().getDescription()
            );
            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));

            // when
            alarmCommandService.createAlarm(request, member.getId());

            // then
            verify(alarmRepository).save(any(AlarmEntity.class));
        }

        @Test
        @DisplayName("회원이 존재하지 않으면 알람 등록 시 예외가 발생한다")
        void fail_memberNotFound() {

            // given
            AlarmFixture fixture = AlarmFixture.ALARM_06;
            AlarmRegisterRequest request = new AlarmRegisterRequest(
                fixture.getAddress(),
                fixture.getLatitude(),
                fixture.getLongitude(),
                fixture.getAlarmPurpose(),
                fixture.getTime(),
                fixture.getRepeatDays().stream().map(Weekday::getDescription).toList(),
                    fixture.getSoundType().getDescription()
            );
            given(memberRepository.findById(MemberFixture.MEMBER_6.getId())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alarmCommandService.createAlarm(request, MemberFixture.MEMBER_6.getId()))
                .isInstanceOf(ApplicationException.class);
        }
    }

/*    @Nested
    @DisplayName("ringAlarm - 알람 울림")
    class RingAlarmTest {

        @Test
        @DisplayName("알람이 울리면 울림 정보가 갱신되고 로그가 저장된다")
        void success() {

            // given
            MemberEntity member = MemberFixture.MEMBER_5.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_05.toMockEntity();
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceEntity.builder()
                .id(1L)
                .alarm(alarm)
                .date(LocalDate.now().minusDays(1))
                .time(alarm.getTime())
                .deactivateType(DeactivateType.NONE)
                .alarmRinging(false)
                .ringingCount(0)
                .reminderSent(false)
                .build();

            given(
                alarmOccurrenceRepository
                    .findTopByAlarmIdAndDeactivateTypeInOrderByDateDescTimeDesc(eq(alarm.getId()), anyList())
            ).willReturn(Optional.of(occurrence));

            // when
            alarmCommandService.ringAlarm(member.getId(), alarm.getId());

            // then
            verify(alarmRingingLogRepository).save(any());
            assertThat(occurrence.isAlarmRinging()).isTrue();
            assertThat(occurrence.getRingingCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("알람 시간이 되지 않았으면 예외를 던진다")
        void fail_notAlarmTime() {

            // given
            MemberEntity member = MemberFixture.MEMBER_5.toMockEntity();
            AlarmEntity alarm = AlarmFixture.ALARM_05.toMockEntity();
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceEntity.builder()
                .id(1L)
                .alarm(alarm)
                .date(LocalDate.now().plusDays(1))
                .time(LocalTime.now().plusHours(1))
                .deactivateType(DeactivateType.NONE)
                .alarmRinging(false)
                .ringingCount(0)
                .reminderSent(false)
                .build();

            given(
                alarmOccurrenceRepository
                    .findTopByAlarmIdAndDeactivateTypeInOrderByDateDescTimeDesc(eq(alarm.getId()), anyList())
            ).willReturn(Optional.of(occurrence));

            // when & then
            assertThatThrownBy(() -> alarmCommandService.ringAlarm(member.getId(), alarm.getId()))
                .isInstanceOf(ApplicationException.class);
        }
    }*/

}