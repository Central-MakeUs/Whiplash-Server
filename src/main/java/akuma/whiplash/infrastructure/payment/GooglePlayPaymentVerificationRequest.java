package akuma.whiplash.infrastructure.payment;

public record GooglePlayPaymentVerificationRequest(
    String purchaseToken,
    String productId
) {
}
