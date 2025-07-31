package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.persistence.entity.AlarmRingingLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmRingingLogRepository extends JpaRepository<AlarmRingingLogEntity, Long> {
    void deleteAllByAlarmOccurrenceId(Long alarmOccurrenceId);
}