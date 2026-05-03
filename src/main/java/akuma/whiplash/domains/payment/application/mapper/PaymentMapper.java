package akuma.whiplash.domains.payment.application.mapper;

import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.payment.domain.constant.PaymentStatus;
import akuma.whiplash.domains.payment.domain.constant.PaymentType;
import akuma.whiplash.domains.payment.persistence.entity.PaymentEntity;

public class PaymentMapper {

    private PaymentMapper() {
        throw new IllegalArgumentException();
    }

    public static PaymentEntity mapToPaymentEntity(
        MemberEntity member,
        AlarmEntity alarm,
        String paymentId,
        PaymentType paymentType,
        PaymentStatus status
    ) {
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
