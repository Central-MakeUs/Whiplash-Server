package akuma.whiplash.infrastructure.payment;

public record AppStorePaymentVerificationRequest(
    String transactionId,
    String productId
) {
}
