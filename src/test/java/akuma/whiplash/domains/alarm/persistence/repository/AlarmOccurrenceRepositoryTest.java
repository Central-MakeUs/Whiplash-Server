package akuma.whiplash.domains.alarm.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.common.config.PersistenceTest;
import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("AlarmOccurrenceRepository Slice Test")
@PersistenceTest
class AlarmOccurrenceRepositoryTest {

    @Autowired
    private AlarmOccurrenceRepository alarmOccurrenceRepository;
    @Autowired
    private AlarmRepository alarmRepository;
    @Autowired
    private MemberRepository memberRepository;

    @Nested
    @DisplayName("findByAlarmIdAndDate - 알람 발생 내역 조회")
    class FindByAlarmIdAndDateTest {

        @Test
        @DisplayName("성공: 알람 ID와 날짜로 발생 내역을 조회하면 해당 내역이 반환된다")
        void success() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_8.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_08.toEntity(member));
            LocalDate today = LocalDate.now();
            AlarmOccurrenceEntity occurrence = AlarmMapper.mapToAlarmOccurrenceForDate(alarm, today);
            alarmOccurrenceRepository.save(occurrence);

            // when
            Optional<AlarmOccurrenceEntity> found = alarmOccurrenceRepository.findByAlarmIdAndDate(alarm.getId(), today);

            // then
            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("실패: 지정된 알람과 날짜의 발생 내역이 없으면 빈 값을 반환한다")
        void fail_notExists() {
            // when
            Optional<AlarmOccurrenceEntity> found = alarmOccurrenceRepository.findByAlarmIdAndDate(999L, LocalDate.now());

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("checkin - 도착 인증")
    class CheckinTest {

        @Test
        @DisplayName("성공: 알람 발생 내역에 체크인하면 체크인 시간이 저장된다")
        void success() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_9.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_09.toEntity(member));
            LocalDate today = LocalDate.now();
            AlarmOccurrenceEntity occurrence = AlarmMapper.mapToAlarmOccurrenceForDate(alarm, today);
            alarmOccurrenceRepository.save(occurrence);

            // when
            occurrence.checkin(LocalDateTime.now());
            alarmOccurrenceRepository.save(occurrence);
            AlarmOccurrenceEntity found = alarmOccurrenceRepository.findByAlarmIdAndDate(alarm.getId(), today).orElseThrow();

            // then
            assertThat(found.getStatus()).isEqualTo(OccurrenceStatus.CHECKIN);
            assertThat(found.getCheckinTime()).isNotNull();
        }
    }

    @Nested
    @DisplayName("findNextScheduledByAlarmIds - 다음 예정 회차 벌크 조회")
    class FindNextScheduledByAlarmIdsTest {

        @Test
        @DisplayName("성공: 알람별 현재 이후 가장 빠른 SCHEDULED 회차만 조회한다")
        void success() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_10.toEntity());
            AlarmEntity firstAlarm = alarmRepository.save(AlarmFixture.ALARM_10.toEntity(member));
            AlarmEntity secondAlarm = alarmRepository.save(AlarmFixture.ALARM_11.toEntity(member));
            LocalDateTime now = LocalDateTime.now();

            AlarmOccurrenceEntity pastScheduled = buildOccurrence(firstAlarm, now.minusDays(1), OccurrenceStatus.SCHEDULED);
            AlarmOccurrenceEntity nextScheduled = buildOccurrence(firstAlarm, now.plusDays(1), OccurrenceStatus.SCHEDULED);
            AlarmOccurrenceEntity laterScheduled = buildOccurrence(firstAlarm, now.plusDays(2), OccurrenceStatus.SCHEDULED);
            AlarmOccurrenceEntity processed = buildOccurrence(secondAlarm, now.plusDays(1), OccurrenceStatus.CHECKIN);
            AlarmOccurrenceEntity secondNextScheduled = buildOccurrence(secondAlarm, now.plusDays(3), OccurrenceStatus.SCHEDULED);
            alarmOccurrenceRepository.saveAll(List.of(
                pastScheduled,
                nextScheduled,
                laterScheduled,
                processed,
                secondNextScheduled
            ));

            // when
            List<AlarmOccurrenceEntity> result = alarmOccurrenceRepository.findNextScheduledByAlarmIds(
                List.of(firstAlarm.getId(), secondAlarm.getId()),
                OccurrenceStatus.SCHEDULED,
                now
            );

            // then
            assertThat(result)
                .extracting(AlarmOccurrenceEntity::getId)
                .containsExactlyInAnyOrder(nextScheduled.getId(), secondNextScheduled.getId());
        }
    }

    @Nested
    @DisplayName("findRingingNotificationTargets - 울림 푸시 대상 조회")
    class FindRingingNotificationTargetsTest {

        @Test
        @DisplayName("성공: RINGING 상태이고 alarmRinging=true인 회차만 조회한다")
        void success() {
            // given
            MemberEntity ringingMember = memberRepository.save(MemberFixture.MEMBER_11.toEntity());
            MemberEntity notRingingMember = memberRepository.save(MemberFixture.MEMBER_12.toEntity());
            MemberEntity scheduledMember = memberRepository.save(MemberFixture.MEMBER_13.toEntity());
            AlarmEntity ringingAlarm = alarmRepository.save(AlarmFixture.ALARM_11.toEntity(ringingMember));
            AlarmEntity notRingingAlarm = alarmRepository.save(AlarmFixture.ALARM_12.toEntity(notRingingMember));
            AlarmEntity scheduledAlarm = alarmRepository.save(AlarmFixture.ALARM_13.toEntity(scheduledMember));
            LocalDateTime now = LocalDateTime.now();

            AlarmOccurrenceEntity ringingOccurrence = buildOccurrence(ringingAlarm, now, OccurrenceStatus.RINGING, true);
            AlarmOccurrenceEntity notRingingOccurrence = buildOccurrence(notRingingAlarm, now, OccurrenceStatus.RINGING, false);
            AlarmOccurrenceEntity scheduledOccurrence = buildOccurrence(scheduledAlarm, now, OccurrenceStatus.SCHEDULED, true);
            alarmOccurrenceRepository.saveAll(List.of(
                ringingOccurrence,
                notRingingOccurrence,
                scheduledOccurrence
            ));

            // when
            List<akuma.whiplash.domains.alarm.application.dto.etc.RingingPushInfo> result =
                alarmOccurrenceRepository.findRingingNotificationTargets(OccurrenceStatus.RINGING);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).alarmId()).isEqualTo(ringingAlarm.getId());
            assertThat(result.get(0).memberId()).isEqualTo(ringingMember.getId());
        }
    }

    private AlarmOccurrenceEntity buildOccurrence(
        AlarmEntity alarm,
        LocalDateTime scheduledAt,
        OccurrenceStatus status
    ) {
        return buildOccurrence(alarm, scheduledAt, status, false);
    }

    private AlarmOccurrenceEntity buildOccurrence(
        AlarmEntity alarm,
        LocalDateTime scheduledAt,
        OccurrenceStatus status,
        boolean alarmRinging
    ) {
        return AlarmOccurrenceEntity.builder()
            .alarm(alarm)
            .occurrenceDate(scheduledAt.toLocalDate())
            .occurrenceTime(scheduledAt.toLocalTime())
            .scheduledAt(scheduledAt)
            .status(status)
            .alarmRinging(alarmRinging)
            .ringingCount(alarmRinging ? 1 : 0)
            .reminderSent(false)
            .build();
    }
}
