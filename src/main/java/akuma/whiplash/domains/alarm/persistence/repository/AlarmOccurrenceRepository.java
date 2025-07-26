package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmOccurrenceRepository extends JpaRepository<AlarmOccurrenceEntity, Long> {

    boolean existsByAlarmIdAndDate(Long alarmId, LocalDate date);
}
