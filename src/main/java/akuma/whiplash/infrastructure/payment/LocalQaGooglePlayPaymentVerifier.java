package akuma.whiplash.infrastructure.payment;

import akuma.whiplash.domains.payment.domain.constant.InAppPaymentPurpose;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "qa"})
public class LocalQaGooglePlayPaymentVerifier implements GooglePlayPaymentVerificationPort {

    // local/qa 전용 mock: 요청 값을 반향하므로 실제 구매 또는 상품 목적을 증명하지 않는다.
    @Override
    public Optional<GooglePlayPaymentVerificationResult> verify(GooglePlayPaymentVerificationRequest request) {
        if (request.purchaseToken() == null || request.purchaseToken().isBlank()
            || request.productId() == null || request.productId().isBlank()) {
            return Optional.empty();
        }
        if (!InAppPaymentPurpose.isSupportedProductId(request.productId())) {
            return Optional.empty();
        }

        return Optional.of(new GooglePlayPaymentVerificationResult(request.purchaseToken(), request.productId()));
    }
}
