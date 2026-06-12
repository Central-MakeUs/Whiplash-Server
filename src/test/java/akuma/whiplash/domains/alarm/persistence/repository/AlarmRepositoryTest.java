package akuma.whiplash.domains.alarm.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.common.config.PersistenceTest;
import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.domain.constant.AlarmStatus;
import akuma.whiplash.domains.alarm.domain.constant.SoundType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository.AlarmOccurrenceBatchTarget;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@PersistenceTest
class AlarmRepositoryTest {

    @Autowired
    private AlarmRepository alarmRepository;
    @Autowired
    private MemberRepository memberRepository;

    @Nested
    @DisplayName("findAllByMemberId - 회원 ID로 알람 조회")
    class FindAllByMemberIdTest {

        @Test
        @DisplayName("성공: 회원 ID로 알람을 조회하면 해당 알람이 반환된다")
        void success() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_7.toEntity());
            alarmRepository.save(AlarmFixture.ALARM_07.toEntity(member));

            // when
            List<AlarmEntity> alarms = alarmRepository.findAllByMemberId(member.getId());

            // then
            assertThat(alarms).hasSize(1);
        }

        @Test
        @DisplayName("실패: 등록되지 않은 회원 ID로 알람을 조회하면 비어있는 목록이 반환된다")
        void fail_memberNotFound() {
            // when
            List<AlarmEntity> alarms = alarmRepository.findAllByMemberId(999L);

            // then
            assertThat(alarms).isEmpty();
        }
    }

    @Nested
    @DisplayName("findBatchTargetsByRepeatDaysLikeAndStatus - 배치 대상 알람 조회")
    class FindBatchTargetsByRepeatDaysLikeAndStatusTest {

        @Test
        @DisplayName("성공: 반복 요일이 일치하는 활성 알람의 ID와 시간만 조회한다")
        void success() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_7.toEntity());
            AlarmEntity activeMondayAlarm = alarmRepository.save(AlarmFixture.ALARM_07.toEntity(member));
            alarmRepository.save(AlarmEntity.builder()
                .member(member)
                .alarmPurpose("주말 알람")
                .time(LocalTime.of(9, 0))
                .repeatDays(List.of(Weekday.SATURDAY))
                .soundType(SoundType.VIBRATION_ONLY)
                .latitude(37.6019)
                .longitude(127.0413)
                .address("서울특별시 성북구 정릉로 77")
                .status(AlarmStatus.ACTIVE)
                .build());
            alarmRepository.save(AlarmEntity.builder()
                .member(member)
                .alarmPurpose("삭제된 월요일 알람")
                .time(LocalTime.of(10, 0))
                .repeatDays(List.of(Weekday.MONDAY))
                .soundType(SoundType.VIBRATION_ONLY)
                .latitude(37.6019)
                .longitude(127.0413)
                .address("서울특별시 성북구 정릉로 77")
                .status(AlarmStatus.DELETED)
                .build());

            // when
            List<AlarmOccurrenceBatchTarget> targets = alarmRepository.findBatchTargetsByRepeatDaysLikeAndStatus(
                "\"MONDAY\"",
                AlarmStatus.ACTIVE.name()
            );

            // then
            assertThat(targets).hasSize(1);
            assertThat(targets.get(0).getAlarmId()).isEqualTo(activeMondayAlarm.getId());
            assertThat(targets.get(0).getAlarmTime()).isEqualTo(activeMondayAlarm.getTime());
        }
    }

}
