package akuma.whiplash.domains.payment.domain.command;

public record StorePaymentProof(
    String paymentId,
    String productId
) {
}
