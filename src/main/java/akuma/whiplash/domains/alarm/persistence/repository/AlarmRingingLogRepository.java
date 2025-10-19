package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.persistence.entity.AlarmRingingLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlarmRingingLogRepository extends JpaRepository<AlarmRingingLogEntity, Long> {
    void deleteAllByAlarmOccurrenceId(Long alarmOccurrenceId);

    @Modifying
    @Query("""
        DELETE FROM AlarmRingingLogEntity arl
        WHERE arl.alarmOccurrence.alarm.member.id = :memberId
    """)
    void deleteByMemberId(@Param("memberId") Long memberId);
}