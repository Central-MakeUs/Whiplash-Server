package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmOccurrenceRepository extends JpaRepository<AlarmOccurrenceEntity, Long> {

    boolean existsByAlarmIdAndDate(Long alarmId, LocalDate date);
    Optional<AlarmOccurrenceEntity> findByAlarmIdAndDate(Long alarmId, LocalDate date);
    Optional<AlarmOccurrenceEntity> findTopByAlarmIdAndDeactivateTypeInOrderByDateDescTimeDesc(
        Long alarmId, List<DeactivateType> deactivateTypes
    );
    List<AlarmOccurrenceEntity> findAllByAlarmId(Long alarmId);
    void deleteAllByAlarmId(Long alarmId);
}
