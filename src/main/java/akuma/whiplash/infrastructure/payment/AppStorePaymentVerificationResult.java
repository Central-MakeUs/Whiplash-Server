package akuma.whiplash.infrastructure.payment;

public record AppStorePaymentVerificationResult(
    String transactionId,
    String productId
) {
}
