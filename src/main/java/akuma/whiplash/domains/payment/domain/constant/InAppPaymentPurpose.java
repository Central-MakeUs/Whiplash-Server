package akuma.whiplash.domains.payment.domain.constant;

import java.util.Arrays;

public enum InAppPaymentPurpose {
    ALARM_OFF("ALARM_OFF"),
    ALARM_DELETE("ALARM_DELETE");

    private final String productId;

    InAppPaymentPurpose(String productId) {
        this.productId = productId;
    }

    public boolean allows(String productId) {
        return this.productId.equals(productId);
    }

    public static boolean isSupportedProductId(String productId) {
        return Arrays.stream(values()).anyMatch(purpose -> purpose.allows(productId));
    }
}
