package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo;
import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushInfo;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlarmOccurrenceRepository extends JpaRepository<AlarmOccurrenceEntity, Long> {

    @Query("""
    SELECT CASE WHEN COUNT(ao) > 0 THEN true ELSE false END
    FROM AlarmOccurrenceEntity ao
    WHERE ao.alarm.id = :alarmId
      AND ao.occurrenceDate = :date
    """)
    boolean existsByAlarmIdAndDate(
        @Param("alarmId") Long alarmId,
        @Param("date") LocalDate date
    );

    @Query("""
    SELECT ao
    FROM AlarmOccurrenceEntity ao
    WHERE ao.alarm.id = :alarmId
      AND ao.occurrenceDate = :date
    """)
    Optional<AlarmOccurrenceEntity> findByAlarmIdAndDate(
        @Param("alarmId") Long alarmId,
        @Param("date") LocalDate date
    );

    @Query("""
    SELECT ao.status
    FROM AlarmOccurrenceEntity ao
    WHERE ao.alarm.id = :alarmId
      AND ao.occurrenceDate = :date
    """)
    Optional<OccurrenceStatus> findStatusByAlarmIdAndDate(
        @Param("alarmId") Long alarmId,
        @Param("date") LocalDate date
    );

    @Query("""
    SELECT ao
    FROM AlarmOccurrenceEntity ao
    WHERE ao.id = :occurrenceId
      AND ao.alarm.id = :alarmId
    """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AlarmOccurrenceEntity> findByIdAndAlarmId(
        @Param("occurrenceId") Long occurrenceId,
        @Param("alarmId") Long alarmId
    );

    @Query("""
    SELECT ao
    FROM AlarmOccurrenceEntity ao
    WHERE ao.id = :occurrenceId
      AND ao.alarm.id = :alarmId
    """)
    Optional<AlarmOccurrenceEntity> findByIdAndAlarmIdForLocationPreparation(
        @Param("occurrenceId") Long occurrenceId,
        @Param("alarmId") Long alarmId
    );

    Optional<AlarmOccurrenceEntity> findTopByAlarmIdAndStatusInOrderByOccurrenceDateDescOccurrenceTimeDesc(
        Long alarmId, List<OccurrenceStatus> statuses
    );

    @Query("""
    SELECT ao
    FROM AlarmOccurrenceEntity ao
    WHERE ao.alarm.id = :alarmId
    """)
    List<AlarmOccurrenceEntity> findAllByAlarmId(@Param("alarmId") Long alarmId);

    @Modifying
    @Query("""
        DELETE FROM AlarmOccurrenceEntity ao
        WHERE ao.alarm.id = :alarmId
    """)
    void deleteByAlarmId(@Param("alarmId") Long alarmId);

    @Query("""
    SELECT ao.alarm.id
    FROM AlarmOccurrenceEntity ao
    WHERE ao.occurrenceDate = :date
    """)
    Set<Long> findAlarmIdsByDate(@Param("date") LocalDate date);

    @Modifying
    @Query("""
        DELETE FROM AlarmOccurrenceEntity ao
        WHERE ao.alarm.member.id = :memberId
    """)
    void deleteByMemberId(@Param("memberId") Long memberId);

    @Query("""
    SELECT new akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo(o.id, m.id, a.id)
    FROM AlarmOccurrenceEntity o
    JOIN o.alarm a
    JOIN a.member m
    WHERE o.occurrenceDate = :date
      AND o.occurrenceTime BETWEEN :start AND :end
      AND o.status = :status
      AND o.reminderSent = false
""")
    List<OccurrencePushInfo> findPreNotificationTargetsSameDay(
        @Param("date") LocalDate date,
        @Param("start") LocalTime start,
        @Param("end") LocalTime end,
        @Param("status") OccurrenceStatus status
    );

    @Query("""
    SELECT new akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo(o.id, m.id, a.id)
    FROM AlarmOccurrenceEntity o
    JOIN o.alarm a
    JOIN a.member m
    WHERE o.occurrenceDate = :date
      AND o.occurrenceTime >= :start
      AND o.status = :status
      AND o.reminderSent = false
""")
    List<OccurrencePushInfo> findPreNotificationTargetsFromTime(
        @Param("date") LocalDate date,
        @Param("start") LocalTime start,
        @Param("status") OccurrenceStatus status
    );

    @Query("""
    SELECT new akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo(o.id, m.id, a.id)
    FROM AlarmOccurrenceEntity o
    JOIN o.alarm a
    JOIN a.member m
    WHERE o.occurrenceDate = :date
      AND o.occurrenceTime <= :end
      AND o.status = :status
      AND o.reminderSent = false
    """)
    List<OccurrencePushInfo> findPreNotificationTargetsUntilTime(
        @Param("date") LocalDate date,
        @Param("end") LocalTime end,
        @Param("status") OccurrenceStatus status
    );

    @Query("""
    SELECT new akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo(o.id, m.id, a.id)
    FROM AlarmOccurrenceEntity o
    JOIN o.alarm a
    JOIN a.member m
    WHERE o.scheduledAt BETWEEN :startInclusive AND :endInclusive
      AND o.status = :status
      AND o.reminderSent = false
    """)
    List<OccurrencePushInfo> findPreNotificationTargetsByScheduledAtBetween(
        @Param("startInclusive") LocalDateTime startInclusive,
        @Param("endInclusive") LocalDateTime endInclusive,
        @Param("status") OccurrenceStatus status
    );

    @Modifying
    @Query("""
        UPDATE AlarmOccurrenceEntity o
        SET o.reminderSent = true
        WHERE o.id IN :ids AND o.reminderSent = false
    """)
    void markReminderSentIn(@Param("ids") Set<Long> ids);

    @Query("""
    SELECT new akuma.whiplash.domains.alarm.application.dto.etc.RingingPushInfo(o.id, a.id, m.id)
    FROM AlarmOccurrenceEntity o
    JOIN o.alarm a
    JOIN a.member m
    WHERE o.alarmRinging = true
      AND o.status = :status
    """)
    List<RingingPushInfo> findRingingNotificationTargets(@Param("status") OccurrenceStatus status);

    @Query("""
    SELECT ao FROM AlarmOccurrenceEntity ao
    JOIN FETCH ao.alarm
    WHERE ao.alarm.id IN :alarmIds
      AND ao.status IN :statuses
      AND ao.occurrenceDate = (
          SELECT MAX(ao2.occurrenceDate)
          FROM AlarmOccurrenceEntity ao2
          WHERE ao2.alarm.id = ao.alarm.id
            AND ao2.status IN :statuses
      )
    """)
    List<AlarmOccurrenceEntity> findLatestProcessedByAlarmIds(
        @Param("alarmIds") List<Long> alarmIds,
        @Param("statuses") List<OccurrenceStatus> statuses
    );

    @Query("""
    SELECT ao FROM AlarmOccurrenceEntity ao
    JOIN FETCH ao.alarm
    WHERE ao.alarm.id IN :alarmIds
      AND ao.occurrenceDate IN :dates
    """)
    List<AlarmOccurrenceEntity> findByAlarmIdsAndOccurrenceDates(
        @Param("alarmIds") List<Long> alarmIds,
        @Param("dates") List<LocalDate> dates
    );

    @Query("""
    SELECT ao FROM AlarmOccurrenceEntity ao
    JOIN FETCH ao.alarm
    WHERE ao.alarm.id IN :alarmIds
      AND ao.status = :status
      AND ao.scheduledAt >= :now
      AND ao.scheduledAt = (
          SELECT MIN(ao2.scheduledAt)
          FROM AlarmOccurrenceEntity ao2
          WHERE ao2.alarm.id = ao.alarm.id
            AND ao2.status = :status
            AND ao2.scheduledAt >= :now
      )
    """)
    List<AlarmOccurrenceEntity> findNextScheduledByAlarmIds(
        @Param("alarmIds") List<Long> alarmIds,
        @Param("status") OccurrenceStatus status,
        @Param("now") LocalDateTime now
    );
}
