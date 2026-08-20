package akuma.whiplash.infrastructure.payment;

import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "qa"})
public class LocalQaGooglePlayPaymentVerifier implements GooglePlayPaymentVerificationPort {

    @Override
    public Optional<GooglePlayPaymentVerificationResult> verify(GooglePlayPaymentVerificationRequest request) {
        if (request.purchaseToken() == null || request.purchaseToken().isBlank()
            || request.productId() == null || request.productId().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new GooglePlayPaymentVerificationResult(request.purchaseToken(), request.productId()));
    }
}
