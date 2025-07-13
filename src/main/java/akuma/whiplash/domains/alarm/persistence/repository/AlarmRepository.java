package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmRepository extends JpaRepository<AlarmEntity, Long> {

}
