package akuma.whiplash.domains.alarm.persistence.repository;

import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlarmRepository extends JpaRepository<AlarmEntity, Long> {

    List<AlarmEntity> findAllByMemberId(Long memberId);

    @Query(value = "SELECT * FROM alarm WHERE repeat_days LIKE %:day%", nativeQuery = true)
    List<AlarmEntity> findByRepeatDaysLike(@Param("day") String day);
}
