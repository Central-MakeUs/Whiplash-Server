package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo;
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
      AND ao.date = :date
    """)
    boolean existsByAlarmIdAndDate(
        @Param("alarmId") Long alarmId,
        @Param("date") LocalDate date
    );

    @Query("""
    SELECT ao
    FROM AlarmOccurrenceEntity ao
    WHERE ao.alarm.id = :alarmId
      AND ao.date = :date
    """)
    Optional<AlarmOccurrenceEntity> findByAlarmIdAndDate(
        @Param("alarmId") Long alarmId,
        @Param("date") LocalDate date
    );

    Optional<AlarmOccurrenceEntity> findTopByAlarmIdAndDateOrderByCreatedAtDesc(
        Long alarmId,
        LocalDate date
    );


    Optional<AlarmOccurrenceEntity> findTopByAlarmIdAndDeactivateTypeInOrderByDateDescTimeDesc(
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
    WHERE ao.date = :date
    """)
    Set<Long> findAlarmIdsByDate(@Param("date") LocalDate date);

    @Modifying
    @Query("""
        DELETE FROM AlarmOccurrenceEntity ao
        WHERE ao.alarm.member.id = :memberId
    """)
    void deleteByMemberId(@Param("memberId") Long memberId);

    @Query("""
        SELECT new akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo(
            o.id, m.id, a.address
        )
        FROM AlarmOccurrenceEntity o
        JOIN o.alarm a
        JOIN a.member m
        WHERE o.date = :date
          AND o.time BETWEEN :start AND :end
          AND o.deactivateType = 'NONE'
          AND o.reminderSent = false
          AND m.pushNotificationPolicy = true
    """)
    List<OccurrencePushInfo> findPushTargetsByTimeRange(
        @Param("date") LocalDate date,
        @Param("start") LocalTime start,
        @Param("end") LocalTime end
    );
}
