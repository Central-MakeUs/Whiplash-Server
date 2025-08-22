package akuma.whiplash.domains.alarm.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.common.config.PersistenceTest;
import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOffLogEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("AlarmOffLogRepository Slice Test")
@PersistenceTest
class AlarmOffLogRepositoryTest {

    @Autowired
    private AlarmOffLogRepository alarmOffLogRepository;

    @Autowired
    private AlarmRepository alarmRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Nested
    @DisplayName("countByMemberIdAndCreatedAtBetween - 주간 알람 OFF 기록 개수 조회")
    class CountByMemberIdAndCreatedAtBetweenTest {

        @Test
        @DisplayName("성공: 범위 내 OFF 기록 수를 반환한다")
        void success() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_7.toEntity());
            AlarmEntity alarm = alarmRepository.save(AlarmFixture.ALARM_07.toEntity(member));
            alarmOffLogRepository.save(AlarmOffLogEntity.builder().alarm(alarm).member(member).build());
            LocalDate today = LocalDate.now();
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            LocalDateTime weekStart = monday.atStartOfDay();
            LocalDateTime now = LocalDateTime.now();

            // when
            long count = alarmOffLogRepository.countByMemberIdAndCreatedAtBetween(member.getId(), weekStart, now);

            // then
            assertThat(count).isEqualTo(1L);
        }

        @Test
        @DisplayName("성공: OFF 기록이 없으면 0을 반환한다")
        void success_returnsZeroWhenNoLogs() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_8.toEntity());
            LocalDate today = LocalDate.now();
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            LocalDateTime weekStart = monday.atStartOfDay();
            LocalDateTime now = LocalDateTime.now();

            // when
            long count = alarmOffLogRepository.countByMemberIdAndCreatedAtBetween(member.getId(), weekStart, now);

            // then
            assertThat(count).isZero();
        }
    }
}