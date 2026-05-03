package akuma.whiplash.domains.payment.persistence.repository;

import akuma.whiplash.domains.payment.persistence.entity.PaymentEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    boolean existsByPaymentId(String paymentId);

    Optional<PaymentEntity> findByPaymentId(String paymentId);
}
