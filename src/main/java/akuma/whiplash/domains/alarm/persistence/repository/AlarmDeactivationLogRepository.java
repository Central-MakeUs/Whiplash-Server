package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.persistence.entity.AlarmDeactivationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlarmDeactivationLogRepository extends JpaRepository<AlarmDeactivationLogEntity, Long> {

    @Modifying
    @Query("""
        DELETE FROM AlarmDeactivationLogEntity adl
        WHERE adl.alarmOccurrence.alarm.id = :alarmId
    """)
    void deleteByAlarmId(@Param("alarmId") Long alarmId);

    @Modifying
    @Query("""
        DELETE FROM AlarmDeactivationLogEntity adl
        WHERE adl.member.id = :memberId
           OR adl.alarmOccurrence.alarm.member.id = :memberId
    """)
    void deleteByMemberId(@Param("memberId") Long memberId);
}
