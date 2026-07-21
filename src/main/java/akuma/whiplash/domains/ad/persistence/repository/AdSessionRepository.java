package akuma.whiplash.domains.ad.persistence.repository;

import akuma.whiplash.domains.ad.persistence.entity.AdSessionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdSessionRepository extends JpaRepository<AdSessionEntity, Long> {

    Optional<AdSessionEntity> findByAdSessionId(String adSessionId);

    boolean existsByTransactionId(String transactionId);
}
