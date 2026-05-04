package akuma.whiplash.domains.alarm.application.service;

import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.DeactivationResult;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeactivationLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeleteLogRepository;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.payment.application.mapper.PaymentMapper;
import akuma.whiplash.domains.payment.domain.constant.PaymentStatus;
import akuma.whiplash.domains.payment.domain.constant.PaymentType;
import akuma.whiplash.domains.payment.persistence.repository.PaymentRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogRecorder {

    private final PaymentRepository paymentRepository;
    private final AlarmDeactivationLogRepository alarmDeactivationLogRepository;
    private final AlarmDeleteLogRepository alarmDeleteLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPaymentDeactivationFailure(
        MemberEntity member,
        AlarmEntity alarm,
        AlarmOccurrenceEntity occurrence,
        String paymentId,
        String deviceId,
        LocalDateTime processedAt,
        String failReason
    ) {
        paymentRepository.save(PaymentMapper.mapToPaymentEntity(
            member,
            alarm,
            paymentId,
            PaymentType.STOP_ALARM,
            PaymentStatus.FAILED
        ));

        alarmDeactivationLogRepository.save(AlarmMapper.mapToPaymentDeactivationLogEntity(
            occurrence,
            member,
            paymentId,
            deviceId,
            processedAt,
            processedAt,
            DeactivationResult.FAIL,
            failReason
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPaymentDeleteFailure(
        MemberEntity member,
        AlarmEntity alarm,
        String paymentId,
        LocalDateTime processedAt,
        String failReason
    ) {
        paymentRepository.save(PaymentMapper.mapToPaymentEntity(
            member,
            alarm,
            paymentId,
            PaymentType.DELETE_ALARM,
            PaymentStatus.FAILED
        ));

        alarmDeleteLogRepository.save(AlarmMapper.mapToPaymentDeleteLogEntity(
            alarm,
            member,
            paymentId,
            failReason,
            processedAt,
            processedAt
        ));
    }
}
