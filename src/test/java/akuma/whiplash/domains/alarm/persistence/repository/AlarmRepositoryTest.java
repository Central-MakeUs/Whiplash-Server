package akuma.whiplash.domains.alarm.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.common.config.PersistenceTest;
import akuma.whiplash.common.config.TestDatabaseConfig;
import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@PersistenceTest
class AlarmRepositoryTest {

    @Autowired
    private AlarmRepository alarmRepository;
    @Autowired
    private MemberRepository memberRepository;

    @DisplayName("회원 ID로 알람을 조회하면 해당 알람이 반환된다")
    @Test
    void findAllByMemberId_returnsAlarms() {
        // given
        MemberEntity member = memberRepository.save(MemberFixture.MEMBER_7.toEntity());
        alarmRepository.save(AlarmFixture.ALARM_07.toEntity(member));

        // when
        List<AlarmEntity> alarms = alarmRepository.findAllByMemberId(member.getId());

        // then
        assertThat(alarms).hasSize(1);
    }

    @DisplayName("등록되지 않은 회원 ID로 알람을 조회하면 비어있는 목록이 반환된다")
    @Test
    void findAllByMemberId_returnsEmpty_whenMemberNotExists() {
        // when
        List<AlarmEntity> alarms = alarmRepository.findAllByMemberId(999L);

        // then
        assertThat(alarms).isEmpty();
    }
}