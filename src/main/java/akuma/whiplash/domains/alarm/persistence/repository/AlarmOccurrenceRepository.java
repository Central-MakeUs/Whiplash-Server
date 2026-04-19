package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo;
import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushInfo;
import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
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

    Optional<AlarmOccurrenceEntity> findTopByAlarmIdAndDeactivateTypeInOrderByOccurrenceDateDescOccurrenceTimeDesc(
        Long alarmId, List<DeactivateType> deactivateTypes
    );

    @Query("""
    SELECT ao
    FROM AlarmOccurrenceEntity ao
    WHERE ao.alarm.id = :alarmId
    """)
    List<AlarmOccurrenceEntity> findAllByAlarmId(@Param("alarmId") Long alarmId);

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
    SELECT new akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo(o.id, m.id, a.address)
    FROM AlarmOccurrenceEntity o
    JOIN o.alarm a
    JOIN a.member m
    WHERE o.occurrenceDate = :date
      AND o.occurrenceTime BETWEEN :start AND :end
      AND o.deactivateType = :status
      AND o.reminderSent = false
""")
    List<OccurrencePushInfo> findPreNotificationTargetsSameDay(
        @Param("date") LocalDate date,
        @Param("start") LocalTime start,
        @Param("end") LocalTime end,
        @Param("status") DeactivateType status
    );

    @Query("""
    SELECT new akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo(o.id, m.id, a.address)
    FROM AlarmOccurrenceEntity o
    JOIN o.alarm a
    JOIN a.member m
    WHERE o.occurrenceDate = :date
      AND o.occurrenceTime >= :start
      AND o.deactivateType = :status
      AND o.reminderSent = false
""")
    List<OccurrencePushInfo> findPreNotificationTargetsFromTime(
        @Param("date") LocalDate date,
        @Param("start") LocalTime start,
        @Param("status") DeactivateType status
    );

    @Query("""
    SELECT new akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo(o.id, m.id, a.address)
    FROM AlarmOccurrenceEntity o
    JOIN o.alarm a
    JOIN a.member m
    WHERE o.occurrenceDate = :date
      AND o.occurrenceTime <= :end
      AND o.deactivateType = :status
      AND o.reminderSent = false
""")
    List<OccurrencePushInfo> findPreNotificationTargetsUntilTime(
        @Param("date") LocalDate date,
        @Param("end") LocalTime end,
        @Param("status") DeactivateType status
    );

    @Modifying
    @Query("""
        UPDATE AlarmOccurrenceEntity o
        SET o.reminderSent = true
        WHERE o.id IN :ids AND o.reminderSent = false
    """)
    void markReminderSentIn(@Param("ids") Set<Long> ids);

    @Query("""
    SELECT new akuma.whiplash.domains.alarm.application.dto.etc.RingingPushInfo(a.id, m.id)
    FROM AlarmOccurrenceEntity o
    JOIN o.alarm a
    JOIN a.member m
    WHERE o.alarmRinging = true
      AND o.deactivateType = :status
    """)
    List<RingingPushInfo> findRingingNotificationTargets(@Param("status") DeactivateType status);
}
