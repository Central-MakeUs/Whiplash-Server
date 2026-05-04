package akuma.whiplash.common.fixture;

import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.payment.domain.constant.PaymentStatus;
import akuma.whiplash.domains.payment.domain.constant.PaymentType;
import akuma.whiplash.domains.payment.persistence.entity.PaymentEntity;
import lombok.Getter;

@Getter
public enum PaymentFixture {

    STOP_ALARM_SUCCESS("payment-success-001", PaymentType.STOP_ALARM, PaymentStatus.SUCCESS),
    STOP_ALARM_FAILED("payment-failed-001", PaymentType.STOP_ALARM, PaymentStatus.FAILED);

    private final String paymentId;
    private final PaymentType paymentType;
    private final PaymentStatus status;

    PaymentFixture(String paymentId, PaymentType paymentType, PaymentStatus status) {
        this.paymentId = paymentId;
        this.paymentType = paymentType;
        this.status = status;
    }

    public PaymentEntity toEntity(MemberEntity member, AlarmEntity alarm) {
        return PaymentEntity.builder()
            .member(member)
            .alarm(alarm)
            .paymentId(paymentId)
            .paymentType(paymentType)
            .amount(0)
            .status(status)
            .build();
    }
}
