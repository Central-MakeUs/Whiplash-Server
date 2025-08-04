package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlarmRepository extends JpaRepository<AlarmEntity, Long> {

    List<AlarmEntity> findAllByMemberIdAndMemberActiveStatusIsTrue(Long memberId);

    @Query(value = "SELECT * FROM alarm WHERE repeat_days LIKE %:day% AND member_active_status = true", nativeQuery = true)
    List<AlarmEntity> findByRepeatDaysLike(@Param("day") String day);

    @Modifying(clearAutomatically = true)
    @Query("""
    UPDATE AlarmEntity a 
    SET a.memberActiveStatus = false 
    WHERE a.member.id = :memberId
    """)
    void updateMemberDeactivateByMemberId(@Param("memberId") Long memberId);
}
