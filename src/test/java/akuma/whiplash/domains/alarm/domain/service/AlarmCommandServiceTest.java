package akuma.whiplash.domains.alarm.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.domain.constant.SoundType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOffLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRingingLogRepository;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.exception.ApplicationException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import akuma.whiplash.global.service.ArchiveService;
import akuma.whiplash.infrastructure.redis.RingingAlarmRedisRepository;
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
    @Mock
    private RingingAlarmRedisRepository ringingAlarmRedisRepository;
    @Mock
    private ArchiveService archiveService;

    @InjectMocks
    private AlarmCommandServiceImpl alarmCommandService;

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

    @Nested
    @DisplayName("checkinAlarm - 도착 인증")
    class CheckinAlarmTest {

        @Test
        @DisplayName("성공: 허용 반경 내에서 도착 인증에 성공한다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_10.toMockEntity();
            AlarmFixture fixture = AlarmFixture.ALARM_10;
            AlarmEntity alarm = AlarmEntity.builder()
                .id(fixture.getId())
                .alarmPurpose(fixture.getAlarmPurpose())
                .time(fixture.getTime())
                .repeatDays(List.of(Weekday.from(LocalDate.now().getDayOfWeek())))
                .soundType(SoundType.ONE)
                .latitude(fixture.getLatitude())
                .longitude(fixture.getLongitude())
                .address(fixture.getAddress())
                .member(member)
                .build();
            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));

            LocalDate today = LocalDate.now();
            AlarmOccurrenceEntity occurrence = AlarmMapper.mapToAlarmOccurrenceForDate(alarm, today);
            given(alarmOccurrenceRepository.findByAlarmIdAndDate(alarm.getId(), today)).willReturn(Optional.of(occurrence));

            AlarmCheckinRequest request = new AlarmCheckinRequest(alarm.getLatitude(), alarm.getLongitude());

            // when
            alarmCommandService.checkinAlarm(member.getId(), alarm.getId(), request);

            // then
            assertThat(occurrence.getDeactivateType()).isEqualTo(DeactivateType.CHECKIN);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 알람이면 예외가 발생한다")
        void fail_alarmNotFound() {
            // given
            given(alarmRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alarmCommandService.checkinAlarm(1L, 1L, new AlarmCheckinRequest(0.0, 0.0)))
                .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("실패: 다른 사용자의 알람이면 예외가 발생한다")
        void fail_permissionDenied() {
            // given
            MemberEntity owner = MemberFixture.MEMBER_11.toMockEntity();
            AlarmFixture fixture = AlarmFixture.ALARM_11;
            AlarmEntity alarm = AlarmEntity.builder()
                .id(fixture.getId())
                .alarmPurpose(fixture.getAlarmPurpose())
                .time(fixture.getTime())
                .repeatDays(fixture.getRepeatDays())
                .soundType(fixture.getSoundType())
                .latitude(fixture.getLatitude())
                .longitude(fixture.getLongitude())
                .address(fixture.getAddress())
                .member(owner)
                .build();
            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));

            AlarmCheckinRequest request = new AlarmCheckinRequest(alarm.getLatitude(), alarm.getLongitude());

            // when & then
            assertThatThrownBy(() -> alarmCommandService.checkinAlarm(999L, alarm.getId(), request))
                .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("실패: 다음 주 알람에는 도착 인증할 수 없다")
        void fail_nextWeek() {
            // given
            MemberEntity member = MemberFixture.MEMBER_12.toMockEntity();
            DayOfWeek today = LocalDate.now().getDayOfWeek();
            DayOfWeek previous = today.minus(1);
            AlarmEntity alarm = AlarmEntity.builder()
                .id(123L)
                .alarmPurpose("test")
                .time(LocalTime.of(7, 0))
                .repeatDays(List.of(Weekday.from(previous)))
                .soundType(SoundType.ONE)
                .latitude(37.0)
                .longitude(127.0)
                .address("test")
                .member(member)
                .build();
            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));

            AlarmCheckinRequest request = new AlarmCheckinRequest(alarm.getLatitude(), alarm.getLongitude());

            // when & then
            assertThatThrownBy(() -> alarmCommandService.checkinAlarm(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("실패: 이미 도착 인증된 알람이면 예외가 발생한다")
        void fail_alreadyDeactivated() {
            // given
            MemberEntity member = MemberFixture.MEMBER_13.toMockEntity();
            AlarmFixture fixture = AlarmFixture.ALARM_13;
            AlarmEntity alarm = AlarmEntity.builder()
                .id(fixture.getId())
                .alarmPurpose(fixture.getAlarmPurpose())
                .time(fixture.getTime())
                .repeatDays(List.of(Weekday.from(LocalDate.now().getDayOfWeek())))
                .soundType(fixture.getSoundType())
                .latitude(fixture.getLatitude())
                .longitude(fixture.getLongitude())
                .address(fixture.getAddress())
                .member(member)
                .build();
            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            LocalDate today = LocalDate.now();
            AlarmOccurrenceEntity occurrence = AlarmMapper.mapToAlarmOccurrenceForDate(alarm, today);
            occurrence.checkin(LocalDateTime.now());
            given(alarmOccurrenceRepository.findByAlarmIdAndDate(alarm.getId(), today)).willReturn(Optional.of(occurrence));

            AlarmCheckinRequest request = new AlarmCheckinRequest(alarm.getLatitude(), alarm.getLongitude());

            // when & then
            assertThatThrownBy(() -> alarmCommandService.checkinAlarm(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("실패: 허용 반경 밖에서 도착 인증을 시도하면 예외가 발생한다")
        void fail_outOfRange() {
            // given
            MemberEntity member = MemberFixture.MEMBER_14.toMockEntity();
            AlarmFixture fixture = AlarmFixture.ALARM_14;
            AlarmEntity alarm = AlarmEntity.builder()
                .id(fixture.getId())
                .alarmPurpose(fixture.getAlarmPurpose())
                .time(fixture.getTime())
                .repeatDays(List.of(Weekday.from(LocalDate.now().getDayOfWeek())))
                .soundType(fixture.getSoundType())
                .latitude(fixture.getLatitude())
                .longitude(fixture.getLongitude())
                .address(fixture.getAddress())
                .member(member)
                .build();
            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            LocalDate today = LocalDate.now();
            AlarmOccurrenceEntity occurrence = AlarmMapper.mapToAlarmOccurrenceForDate(alarm, today);
            given(alarmOccurrenceRepository.findByAlarmIdAndDate(alarm.getId(), today)).willReturn(Optional.of(occurrence));

            AlarmCheckinRequest request = new AlarmCheckinRequest(alarm.getLatitude() + 1, alarm.getLongitude() + 1);

            // when & then
            assertThatThrownBy(() -> alarmCommandService.checkinAlarm(member.getId(), alarm.getId(), request))
                .isInstanceOf(ApplicationException.class);
        }
    }

    @Nested
    @DisplayName("removeAlarm - 알람 삭제")
    class RemoveAlarmTest {

        @Test
        @DisplayName("성공: 알람을 삭제한다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_7.toMockEntity();
            AlarmFixture fixture = AlarmFixture.ALARM_07;
            AlarmEntity alarm = AlarmEntity.builder()
                .id(fixture.getId())
                .alarmPurpose(fixture.getAlarmPurpose())
                .time(fixture.getTime())
                .repeatDays(fixture.getRepeatDays())
                .soundType(fixture.getSoundType())
                .latitude(fixture.getLatitude())
                .longitude(fixture.getLongitude())
                .address(fixture.getAddress())
                .member(member)
                .build();
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceEntity.builder()
                .id(1L)
                .alarm(alarm)
                .date(LocalDate.now())
                .time(LocalTime.NOON)
                .deactivateType(DeactivateType.NONE)
                .alarmRinging(false)
                .ringingCount(0)
                .reminderSent(false)
                .build();
            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository.findAllByAlarmId(alarm.getId())).willReturn(List.of(occurrence));

            // when
            alarmCommandService.removeAlarm(member.getId(), alarm.getId(), "사유");

            // then
            verify(alarmRingingLogRepository).deleteAllByAlarmOccurrenceId(occurrence.getId());
            verify(alarmOccurrenceRepository).deleteAll(List.of(occurrence));
            verify(alarmOffLogRepository).deleteAllByAlarmId(alarm.getId());
            verify(alarmRepository).delete(alarm);
        }

        @Test
        @DisplayName("실패: 알람이 존재하지 않으면 예외를 던진다")
        void fail_alarmNotFound() {
            // given
            given(alarmRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alarmCommandService.removeAlarm(1L, 1L, "사유"))
                .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("실패: 소유자가 아니면 예외를 던진다")
        void fail_invalidOwner() {
            // given
            MemberEntity owner = MemberFixture.MEMBER_9.toMockEntity();
            AlarmFixture fixture = AlarmFixture.ALARM_09;
            AlarmEntity alarm = AlarmEntity.builder()
                .id(fixture.getId())
                .alarmPurpose(fixture.getAlarmPurpose())
                .time(fixture.getTime())
                .repeatDays(fixture.getRepeatDays())
                .soundType(fixture.getSoundType())
                .latitude(fixture.getLatitude())
                .longitude(fixture.getLongitude())
                .address(fixture.getAddress())
                .member(owner)
                .build();
            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));

            // when & then
            assertThatThrownBy(() -> alarmCommandService.removeAlarm(MemberFixture.MEMBER_10.getId(), alarm.getId(), "사유"))
                .isInstanceOf(ApplicationException.class);
        }
    }

    @Nested
    @DisplayName("ringAlarm - 알람 울림")
    class RingAlarmTest {

        @Test
        @DisplayName("알람이 울리면 alarmRinging=true, 로그 저장, Redis 적재가 모두 수행된다")
        void success() {
            // given
            MemberEntity member = MemberFixture.MEMBER_5.toMockEntity();
            AlarmFixture fixture = AlarmFixture.ALARM_05;
            AlarmEntity alarm = AlarmEntity.builder()
                    .id(fixture.getId())
                    .alarmPurpose(fixture.getAlarmPurpose())
                    .time(fixture.getTime())
                    .repeatDays(fixture.getRepeatDays())
                    .soundType(fixture.getSoundType())
                    .latitude(fixture.getLatitude())
                    .longitude(fixture.getLongitude())
                    .address(fixture.getAddress())
                    .member(member) // toEntity()는 id가 없으므로 toMockEntity()로 생성한 member 직접 주입
                    .build();
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceEntity.builder()
                    .id(1L)
                    .alarm(alarm)
                    .date(LocalDate.now().minusDays(1)) // 과거 날짜 → 알람 시각 이미 지남
                    .time(LocalTime.of(0, 0))
                    .deactivateType(DeactivateType.NONE)
                    .alarmRinging(false)
                    .ringingCount(0)
                    .reminderSent(false)
                    .build();

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(alarmOccurrenceRepository
                    .findTopByAlarmIdAndDeactivateTypeInOrderByDateDescTimeDesc(eq(alarm.getId()), anyList()))
                    .willReturn(Optional.of(occurrence));

            // when
            alarmCommandService.ringAlarm(member.getId(), alarm.getId());

            // then
            assertThat(occurrence.isAlarmRinging()).isTrue();          // DB 컬럼 변경
            assertThat(occurrence.getRingingCount()).isEqualTo(1);
            verify(alarmRingingLogRepository).save(any());             // 로그 저장
            verify(ringingAlarmRedisRepository).add(                   // Redis ZADD
                    eq(alarm.getId()), eq(member.getId()), anyLong());
        }

        @Test
        @DisplayName("알람 시간이 되지 않았으면 예외를 던진다")
        void fail_notAlarmTime() {

            // given
            MemberEntity member = MemberFixture.MEMBER_5.toMockEntity();
            AlarmFixture fixture = AlarmFixture.ALARM_05;
            AlarmEntity alarm = AlarmEntity.builder()
                    .id(fixture.getId())
                    .alarmPurpose(fixture.getAlarmPurpose())
                    .time(fixture.getTime())
                    .repeatDays(fixture.getRepeatDays())
                    .soundType(fixture.getSoundType())
                    .latitude(fixture.getLatitude())
                    .longitude(fixture.getLongitude())
                    .address(fixture.getAddress())
                    .member(member) // toEntity()는 id가 없으므로 toMockEntity()로 생성한 member 직접 주입
                    .build();
            AlarmOccurrenceEntity occurrence = AlarmOccurrenceEntity.builder()
                .id(1L)
                .alarm(alarm)
                .date(LocalDate.now().plusDays(1))  // 미래 날짜 → 아직 울릴 시간 아님
                .time(LocalTime.now().plusHours(1))
                .deactivateType(DeactivateType.NONE)
                .alarmRinging(false)
                .ringingCount(0)
                .reminderSent(false)
                .build();

            given(alarmRepository.findById(alarm.getId())).willReturn(Optional.of(alarm));
            given(
                alarmOccurrenceRepository
                    .findTopByAlarmIdAndDeactivateTypeInOrderByDateDescTimeDesc(eq(alarm.getId()), anyList())
            ).willReturn(Optional.of(occurrence));

            // when & then
            assertThatThrownBy(() -> alarmCommandService.ringAlarm(member.getId(), alarm.getId()))
                .isInstanceOf(ApplicationException.class);
        }
    }
}
