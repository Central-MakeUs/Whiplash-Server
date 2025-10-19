package akuma.whiplash.domains.alarm.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import akuma.whiplash.common.config.PersistenceTest;
import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOffLogEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.util.date.DateUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private AlarmOffLogRepository alarmOffLogRepository;
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
            assertThat(found.getDeactivateType()).isEqualTo(DeactivateType.CHECKIN);
            assertThat(found.getCheckinTime()).isNotNull();
        }
    }

    @Nested
    @DisplayName("countByMemberIdAndCreatedAtBetween - 주간 알람 끈 횟수 조회")
    class CountByMemberIdAndCreatedAtBetweenTest {

        @Test
        @DisplayName("성공: 회원의 주간 알람 끈 횟수를 반환한다")
        void success() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_11.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_11.toEntity(member));
            alarmOffLogRepository.save(AlarmOffLogEntity.builder().alarm(alarm).member(member).build());

            LocalDate weekStart = DateUtil.getWeekStartDate(LocalDate.now());
            LocalDate weekEnd = DateUtil.getWeekEndDate(weekStart);

            // when
            long count = alarmOffLogRepository.countByMemberIdAndCreatedAtBetween(
                member.getId(),
                weekStart.atStartOfDay(),
                weekEnd.plusDays(1).atStartOfDay()
            );

            // then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("성공: 해당 주에 끈 기록이 없으면 0을 반환한다")
        void success_zeroWhenNoLog() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_12.toEntity());
            alarmRepository.save(AlarmFixture.ALARM_12.toEntity(member));

            LocalDate weekStart = DateUtil.getWeekStartDate(LocalDate.now());
            LocalDate weekEnd = DateUtil.getWeekEndDate(weekStart);

            // when
            long count = alarmOffLogRepository.countByMemberIdAndCreatedAtBetween(
                member.getId(),
                weekStart.atStartOfDay(),
                weekEnd.plusDays(1).atStartOfDay()
            );

            // then
            assertThat(count).isZero();
        }
    }
}
