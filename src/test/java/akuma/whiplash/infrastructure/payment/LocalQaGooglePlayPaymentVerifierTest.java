package akuma.whiplash.infrastructure.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Local/QA Google Play 결제 검증을 확인한다")
class LocalQaGooglePlayPaymentVerifierTest {

    private final LocalQaGooglePlayPaymentVerifier verifier = new LocalQaGooglePlayPaymentVerifier();

    @Nested
    @DisplayName("verify - local/qa Google Play 거래 검증")
    class VerifyTest {

        @Test
        @DisplayName("성공: 구매 토큰과 상품 정보가 있으면 거래 정보를 반환한다")
        void success() {
            // given
            GooglePlayPaymentVerificationRequest request = new GooglePlayPaymentVerificationRequest(
                "google-play-purchase-token", "ALARM_OFF"
            );

            // when
            var result = verifier.verify(request);

            // then
            assertThat(result).hasValue(new GooglePlayPaymentVerificationResult(
                "google-play-purchase-token", "ALARM_OFF"
            ));
        }

        @Test
        @DisplayName("실패: 구매 토큰이 비어 있으면 거래를 허용하지 않는다")
        void fail_blankPurchaseToken() {
            // given
            GooglePlayPaymentVerificationRequest request = new GooglePlayPaymentVerificationRequest("", "ALARM_OFF");

            // when
            var result = verifier.verify(request);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("실패: 지원하지 않는 상품 ID면 거래를 허용하지 않는다")
        void fail_unsupportedProductId() {
            // given
            GooglePlayPaymentVerificationRequest request = new GooglePlayPaymentVerificationRequest(
                "google-play-purchase-token", "UNSUPPORTED"
            );

            // when
            var result = verifier.verify(request);

            // then
            assertThat(result).isEmpty();
        }
    }
}
