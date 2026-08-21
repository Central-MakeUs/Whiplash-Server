package akuma.whiplash.infrastructure.payment;

import java.util.Optional;

public interface AppStorePaymentVerificationPort {

    Optional<AppStorePaymentVerificationResult> verify(AppStorePaymentVerificationRequest request);
}
