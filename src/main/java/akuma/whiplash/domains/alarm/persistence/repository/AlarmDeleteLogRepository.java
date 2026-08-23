package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.persistence.entity.AlarmDeleteLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlarmDeleteLogRepository extends JpaRepository<AlarmDeleteLogEntity, Long> {

    @Modifying
    @Query("""
        DELETE FROM AlarmDeleteLogEntity adl
        WHERE adl.member.id = :memberId
           OR adl.alarm.member.id = :memberId
    """)
    void deleteByMemberId(@Param("memberId") Long memberId);
}
