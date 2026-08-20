package akuma.whiplash.infrastructure.payment;

import java.util.Optional;

public interface GooglePlayPaymentVerificationPort {

    Optional<GooglePlayPaymentVerificationResult> verify(GooglePlayPaymentVerificationRequest request);
}
