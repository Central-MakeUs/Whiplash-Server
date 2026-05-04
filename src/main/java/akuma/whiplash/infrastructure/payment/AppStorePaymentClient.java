package akuma.whiplash.infrastructure.payment;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "dev", "qa"})
public class AppStorePaymentClient implements PaymentVerificationPort {

    @Override
    public boolean verify(String transactionId) {
        // TODO: prod에서는 App Store inApps/v1/transactions 기반 실제 검증 구현체를 등록한다.
        return transactionId != null && !transactionId.isBlank();
    }

    @Override
    public void consume(String transactionId) {
        // App Store는 서버 consume 단계가 없고 클라이언트 finishTransaction 책임으로 둔다.
    }

    @Override
    public String supportedPlatform() {
        return "IOS";
    }
}
