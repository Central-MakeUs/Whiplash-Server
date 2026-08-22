package akuma.whiplash.domains.payment.persistence.repository;

import akuma.whiplash.domains.payment.persistence.entity.PaymentEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    boolean existsByPaymentId(String paymentId);

    Optional<PaymentEntity> findByPaymentId(String paymentId);

    @Modifying
    @Query("""
        DELETE FROM PaymentEntity p
        WHERE p.member.id = :memberId
           OR p.alarm.member.id = :memberId
    """)
    void deleteByMemberId(@Param("memberId") Long memberId);
}
