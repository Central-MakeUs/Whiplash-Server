package akuma.whiplash.infrastructure.payment;

public interface PaymentVerificationPort {

    boolean verify(String paymentId);

    void consume(String paymentId);

    String supportedPlatform();
}
