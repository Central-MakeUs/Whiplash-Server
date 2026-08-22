package akuma.whiplash.domains.ad.persistence.repository;

import akuma.whiplash.domains.ad.persistence.entity.AdSessionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdSessionRepository extends JpaRepository<AdSessionEntity, Long> {

    Optional<AdSessionEntity> findByAdSessionId(String adSessionId);

    boolean existsByTransactionId(String transactionId);

    @Modifying
    @Query("""
        DELETE FROM AdSessionEntity ads
        WHERE ads.member.id = :memberId
           OR ads.alarm.member.id = :memberId
    """)
    void deleteByMemberId(@Param("memberId") Long memberId);
}
