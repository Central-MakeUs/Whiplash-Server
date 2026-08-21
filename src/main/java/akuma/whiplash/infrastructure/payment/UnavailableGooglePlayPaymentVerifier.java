package akuma.whiplash.infrastructure.payment;

import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local & !qa")
public class UnavailableGooglePlayPaymentVerifier implements GooglePlayPaymentVerificationPort {

    @Override
    public Optional<GooglePlayPaymentVerificationResult> verify(GooglePlayPaymentVerificationRequest request) {
        return Optional.empty();
    }
}
