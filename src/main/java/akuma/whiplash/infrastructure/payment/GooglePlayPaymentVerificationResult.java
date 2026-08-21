package akuma.whiplash.infrastructure.payment;

public record GooglePlayPaymentVerificationResult(
    String purchaseToken,
    String productId
) {
}
