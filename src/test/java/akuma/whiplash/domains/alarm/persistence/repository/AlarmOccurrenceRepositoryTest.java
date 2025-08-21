package akuma.whiplash.domains.alarm.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.common.config.PersistenceTest;
import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushInfo;
import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@PersistenceTest
class AlarmOccurrenceRepositoryTest {

    @Autowired
    private AlarmOccurrenceRepository alarmOccurrenceRepository;
    @Autowired
    private AlarmRepository alarmRepository;
    @Autowired
    private MemberRepository memberRepository;

    @DisplayName("알람이 울리고 있을 때 알람 울림 푸시 알림 대상을 조회하면 회원과 알람 정보가 반환된다")
    @Test
    void findRingingNotificationTargets_returnsInfo_whenAlarmRinging() {
        // given
        MemberEntity member = memberRepository.save(MemberFixture.MEMBER_7.toEntity());
        AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_07.toEntity(member));
        alarmOccurrenceRepository.save(AlarmOccurrenceEntity.builder()
            .alarm(alarm)
            .date(LocalDate.now())
            .time(alarm.getTime())
            .deactivateType(DeactivateType.NONE)
            .alarmRinging(true)
            .ringingCount(1)
            .reminderSent(false)
            .build());

        // when
        List<RingingPushInfo> infos = alarmOccurrenceRepository.findRingingNotificationTargets(DeactivateType.NONE);

        // then
        assertThat(infos).hasSize(1);
        RingingPushInfo info = infos.get(0);
        assertThat(info.alarmId()).isEqualTo(alarm.getId());
        assertThat(info.memberId()).isEqualTo(member.getId());
    }

    @DisplayName("알람이 울리고 있지 않으면 알람 울림 푸시 알림 대상이 조회되지 않는다")
    @Test
    void findRingingNotificationTargets_returnsEmpty_whenNoRinging() {
        // when
        List<RingingPushInfo> infos = alarmOccurrenceRepository.findRingingNotificationTargets(DeactivateType.NONE);

        // then
        assertThat(infos).isEmpty();
    }
}
