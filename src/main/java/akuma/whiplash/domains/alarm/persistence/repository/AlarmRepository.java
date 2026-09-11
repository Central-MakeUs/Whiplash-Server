package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.domain.constant.AlarmStatus;
import akuma.whiplash.domains.alarm.domain.constant.LocationSource;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a
        FROM AlarmEntity a
        JOIN FETCH a.member
        WHERE a.id = :alarmId
    """)
    Optional<AlarmEntity> findByIdWithMemberForUpdate(@Param("alarmId") Long alarmId);

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

    @Query(value = """
        SELECT
            a.id AS alarmId,
            a.member_id AS memberId,
            a.alarm_time AS alarmTime,
            a.repeat_days AS repeatDays,
            COALESCE((
                SELECT md.time_zone
                FROM member_device md
                WHERE md.member_id = a.member_id
                  AND md.is_logged_in = true
                ORDER BY md.last_active_at DESC
                LIMIT 1
            ), 'Asia/Seoul') AS timeZone
        FROM alarm a
        WHERE a.status = :status
    """, nativeQuery = true)
    List<AlarmOccurrenceBatchTarget> findBatchTargetsByStatus(@Param("status") String status);

    @Modifying
    @Query("""
        DELETE FROM AlarmEntity a
        WHERE a.member.id = :memberId
    """)
    void deleteByMemberId(@Param("memberId") Long memberId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        DELETE FROM AlarmEntity a
        WHERE a.id = :alarmId
    """)
    void deleteByAlarmId(@Param("alarmId") Long alarmId);

    @Modifying
    @Query("""
        UPDATE AlarmEntity a
        SET a.latitude = NULL,
            a.longitude = NULL,
            a.address = NULL,
            a.locationCachedAt = NULL
        WHERE a.locationSource = :locationSource
          AND a.locationCachedAt <= :expiresAt
    """)
    int updateLocationCachesBySourceAndCachedAtBefore(
        @Param("locationSource") LocationSource locationSource,
        @Param("expiresAt") LocalDateTime expiresAt
    );

    interface AlarmOccurrenceBatchTarget {
        Long getAlarmId();

        Long getMemberId();

        LocalTime getAlarmTime();

        String getRepeatDays();

        String getTimeZone();
    }
}
