package akuma.whiplash.infrastructure.payment;

import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local & !qa")
public class UnavailableAppStorePaymentVerifier implements AppStorePaymentVerificationPort {

    @Override
    public Optional<AppStorePaymentVerificationResult> verify(AppStorePaymentVerificationRequest request) {
        return Optional.empty();
    }
}
