package akuma.whiplash.domains.ad.persistence.repository;

import akuma.whiplash.domains.ad.persistence.entity.AdSessionEntity;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdSessionRepository extends JpaRepository<AdSessionEntity, Long> {

    Optional<AdSessionEntity> findByAdSessionId(String adSessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ads
        FROM AdSessionEntity ads
        WHERE ads.adSessionId = :adSessionId
    """)
    Optional<AdSessionEntity> findByAdSessionIdForUpdate(@Param("adSessionId") String adSessionId);

    boolean existsByTransactionId(String transactionId);

    @Modifying
    @Query("""
        DELETE FROM AdSessionEntity ads
        WHERE ads.alarm.id = :alarmId
    """)
    void deleteByAlarmId(@Param("alarmId") Long alarmId);

    @Modifying
    @Query("""
        DELETE FROM AdSessionEntity ads
        WHERE ads.member.id = :memberId
           OR ads.alarm.member.id = :memberId
    """)
    void deleteByMemberId(@Param("memberId") Long memberId);
}
