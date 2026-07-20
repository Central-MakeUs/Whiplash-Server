package akuma.whiplash.infrastructure.payment;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "dev", "qa"})
public class GooglePlayPaymentClient implements PaymentVerificationPort {

    @Override
    public boolean verify(String purchaseToken) {
        // TODO: prod에서는 Google Play purchases.products.get 기반 실제 검증 구현체를 등록한다.
        return purchaseToken != null && !purchaseToken.isBlank();
    }

    @Override
    public void consume(String purchaseToken) {
        // Google Play consume API 연동 전까지는 서버 상태 전환 후 best-effort no-op으로 처리한다.
    }

    @Override
    public String supportedPlatform() {
        return "ANDROID";
    }
}
