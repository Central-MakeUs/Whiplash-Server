package akuma.whiplash.domains.payment.domain.constant;

public enum AppStorePaymentPurpose {
    ALARM_OFF,
    ALARM_DELETE;

    public boolean allows(String productId) {
        return name().equals(productId);
    }
}
