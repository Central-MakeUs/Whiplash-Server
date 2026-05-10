package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.persistence.entity.AlarmDeleteLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmDeleteLogRepository extends JpaRepository<AlarmDeleteLogEntity, Long> {
}
