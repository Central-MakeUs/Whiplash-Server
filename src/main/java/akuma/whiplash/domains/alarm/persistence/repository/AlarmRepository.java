package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.domain.constant.AlarmStatus;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlarmRepository extends JpaRepository<AlarmEntity, Long> {

    @Query("""
        SELECT a
        FROM AlarmEntity a
        JOIN FETCH a.member
        WHERE a.id = :alarmId
    """)
    Optional<AlarmEntity> findByIdWithMember(@Param("alarmId") Long alarmId);

    List<AlarmEntity> findAllByMemberId(Long memberId);

    List<AlarmEntity> findAllByMemberIdAndStatusNot(Long memberId, AlarmStatus status);

    boolean existsByMemberIdAndAlarmPurpose(Long memberId, String alarmPurpose);

    @Query(value = """
        SELECT
            id AS alarmId,
            alarm_time AS alarmTime
        FROM alarm
        WHERE repeat_days LIKE %:day%
          AND status = :status
    """, nativeQuery = true)
    List<AlarmOccurrenceBatchTarget> findBatchTargetsByRepeatDaysLikeAndStatus(
        @Param("day") String day,
        @Param("status") String status
    );

    @Modifying
    @Query("""
        DELETE FROM AlarmEntity a
        WHERE a.member.id = :memberId
    """)
    void deleteByMemberId(@Param("memberId") Long memberId);

    interface AlarmOccurrenceBatchTarget {
        Long getAlarmId();

        LocalTime getAlarmTime();
    }
}
