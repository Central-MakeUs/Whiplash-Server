package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import java.time.LocalDate;
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
      AND ao.memberActiveStatus = true
    """)
    boolean existsByAlarmIdAndDateIfActive(
        @Param("alarmId") Long alarmId,
        @Param("date") LocalDate date
    );

    @Query("""
    SELECT ao
    FROM AlarmOccurrenceEntity ao
    WHERE ao.alarm.id = :alarmId
      AND ao.date = :date
      AND ao.memberActiveStatus = true
    """)
    Optional<AlarmOccurrenceEntity> findByAlarmIdAndDateIfActive(
        @Param("alarmId") Long alarmId,
        @Param("date") LocalDate date
    );

    Optional<AlarmOccurrenceEntity> findTopByAlarmIdAndDeactivateTypeInAndMemberActiveStatusIsTrueOrderByDateDescTimeDesc(
        Long alarmId, List<DeactivateType> deactivateTypes
    );

    @Query("""
    SELECT ao
    FROM AlarmOccurrenceEntity ao
    WHERE ao.alarm.id = :alarmId
      AND ao.memberActiveStatus = true
    """)
    List<AlarmOccurrenceEntity> findAllByAlarmIdIfActive(@Param("alarmId") Long alarmId);

    @Query("""
    SELECT ao.alarm.id
    FROM AlarmOccurrenceEntity ao
    WHERE ao.date = :date
      AND ao.memberActiveStatus = true
    """)
    Set<Long> findAlarmIdsByDateIfActive(@Param("date") LocalDate date);

    @Modifying(clearAutomatically = true)
    @Query("""
    UPDATE AlarmOccurrenceEntity ao 
    SET ao.memberActiveStatus = false 
    WHERE ao.alarm.id IN (
        SELECT a.id FROM AlarmEntity a WHERE a.member.id = :memberId
    )
    """)
    void updateMemberDeactivateByMemberId(@Param("memberId") Long memberId);
}
