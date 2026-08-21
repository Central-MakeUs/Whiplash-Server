package akuma.whiplash.infrastructure.payment;

import akuma.whiplash.domains.payment.domain.constant.InAppPaymentPurpose;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "qa"})
public class XcodeAppStorePaymentVerifier implements AppStorePaymentVerificationPort {

    @Override
    public Optional<AppStorePaymentVerificationResult> verify(AppStorePaymentVerificationRequest request) {
        if (request.transactionId() == null || request.transactionId().isBlank()
            || request.productId() == null || request.productId().isBlank()) {
            return Optional.empty();
        }
        if (!InAppPaymentPurpose.isSupportedProductId(request.productId())) {
            return Optional.empty();
        }

        return Optional.of(new AppStorePaymentVerificationResult(request.transactionId(), request.productId()));
    }
}
