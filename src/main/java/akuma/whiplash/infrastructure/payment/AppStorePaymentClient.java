package akuma.whiplash.infrastructure.payment;

import org.springframework.stereotype.Component;

@Component
public class AppStorePaymentClient implements PaymentVerificationPort {

    @Override
    public boolean verify(String transactionId) {
        // TODO: App Store inApps/v1/transactions 연동 전까지 local/dev 검증용 스텁으로 사용한다.
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
