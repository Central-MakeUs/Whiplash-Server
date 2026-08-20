package akuma.whiplash.infrastructure.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("XcodeAppStorePaymentVerifier Test")
class XcodeAppStorePaymentVerifierTest {

    private final XcodeAppStorePaymentVerifier verifier = new XcodeAppStorePaymentVerifier();

    @Nested
    @DisplayName("verify - Xcode StoreKit 거래 검증")
    class VerifyTest {

        @Test
        @DisplayName("성공: Xcode 거래이면 거래와 상품 정보를 반환한다")
        void success() {
            // given
            AppStorePaymentVerificationRequest request = new AppStorePaymentVerificationRequest(
                "xcode-transaction-id", "ALARM_OFF"
            );

            // when
            var result = verifier.verify(request);

            // then
            assertThat(result).hasValue(new AppStorePaymentVerificationResult(
                "xcode-transaction-id", "ALARM_OFF"
            ));
        }

        @Test
        @DisplayName("실패: 거래 ID가 없으면 허용하지 않는다")
        void fail_blankTransactionId() {
            // given
            AppStorePaymentVerificationRequest request = new AppStorePaymentVerificationRequest(
                "", "ALARM_OFF"
            );

            // when
            var result = verifier.verify(request);

            // then
            assertThat(result).isEmpty();
        }
    }
}
