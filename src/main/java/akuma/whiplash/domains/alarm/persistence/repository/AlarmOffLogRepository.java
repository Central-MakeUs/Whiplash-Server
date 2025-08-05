package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.persistence.entity.AlarmOffLogEntity;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlarmOffLogRepository extends JpaRepository<AlarmOffLogEntity, Long> {
    long countByMemberIdAndCreatedAtBetween(
        Long memberId,
        LocalDateTime start,
        LocalDateTime end
    );
    void deleteAllByAlarmId(Long alarmId);

    @Modifying
    @Query("""
        DELETE FROM AlarmOffLogEntity aol
        WHERE aol.member.id = :memberId
    """)
    void deleteByMemberId(@Param("memberId") Long memberId);
}
