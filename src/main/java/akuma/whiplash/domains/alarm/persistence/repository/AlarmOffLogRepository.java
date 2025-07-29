package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.persistence.entity.AlarmOffLogEntity;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmOffLogRepository extends JpaRepository<AlarmOffLogEntity, Long> {
    long countByAlarmIdAndMemberIdAndCreatedAtBetween(
        Long alarmId,
        Long memberId,
        LocalDateTime start,
        LocalDateTime end
    );
}
