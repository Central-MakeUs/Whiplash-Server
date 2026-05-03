package akuma.whiplash.infrastructure.payment;

import org.springframework.stereotype.Component;

@Component
public class GooglePlayPaymentClient implements PaymentVerificationPort {

    @Override
    public boolean verify(String purchaseToken) {
        // TODO: Google Play purchases.products.get 연동 전까지 local/dev 검증용 스텁으로 사용한다.
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
