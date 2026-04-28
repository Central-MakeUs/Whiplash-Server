package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.persistence.entity.AlarmDeactivationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmDeactivationLogRepository extends JpaRepository<AlarmDeactivationLogEntity, Long> {
}
