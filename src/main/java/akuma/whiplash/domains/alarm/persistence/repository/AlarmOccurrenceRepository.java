package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlarmOccurrenceRepository extends JpaRepository<AlarmOccurrenceEntity, Long> {

    boolean existsByAlarmIdAndDate(Long alarmId, LocalDate date);
    Optional<AlarmOccurrenceEntity> findByAlarmIdAndDate(Long alarmId, LocalDate date);
    Optional<AlarmOccurrenceEntity> findTopByAlarmIdAndDeactivateTypeInOrderByDateDescTimeDesc(
        Long alarmId, List<DeactivateType> deactivateTypes
    );
    List<AlarmOccurrenceEntity> findAllByAlarmId(Long alarmId);

    @Query("SELECT ao.alarm.id FROM AlarmOccurrenceEntity ao WHERE ao.date = :date")
    Set<Long> findAlarmIdsByDate(@Param("date") LocalDate date);
}
