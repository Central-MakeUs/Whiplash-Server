package akuma.whiplash.domains.alarm.domain.service;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.*;

import akuma.whiplash.domains.alarm.application.event.AlarmCheckinCompletedEvent;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmDeleteByAdRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmDeleteByPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmCheckinResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmPaymentResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.application.service.AuditLogRecorder;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.domain.constant.DeactivationResult;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.domain.util.AlarmScheduleCalculator;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmRingingLogEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeactivationLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeleteLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRingingLogRepository;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.domains.device.exception.DeviceErrorCode;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberDeviceEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.domains.payment.application.mapper.PaymentMapper;
import akuma.whiplash.domains.payment.domain.constant.PaymentStatus;
import akuma.whiplash.domains.payment.domain.constant.PaymentType;
import akuma.whiplash.domains.payment.exception.PaymentErrorCode;
import akuma.whiplash.domains.payment.persistence.repository.PaymentRepository;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.code.CommonErrorCode;
import akuma.whiplash.global.util.date.TimeProvider;
import akuma.whiplash.infrastructure.payment.PaymentVerificationPort;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AlarmCommandServiceImpl implements AlarmCommandService {

    private final List<PaymentVerificationPort> paymentVerificationPorts;
    private final AuditLogRecorder auditLogRecorder;
    private final AlarmRepository alarmRepository;
    private final AlarmOccurrenceRepository alarmOccurrenceRepository;
    private final AlarmRingingLogRepository alarmRingingLogRepository;
    private final AlarmDeactivationLogRepository alarmDeactivationLogRepository;
    private final AlarmDeleteLogRepository alarmDeleteLogRepository;
    private final MemberRepository memberRepository;
    private final MemberDeviceRepository memberDeviceRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TimeProvider timeProvider;

    private static final double CHECKIN_RADIUS_METERS = 50.0;

    @Override
    public CreateAlarmResponse createAlarm(AlarmRegisterRequest request, Long memberId, String deviceId) {
        MemberEntity memberEntity = findMemberById(memberId);

        boolean exists = alarmRepository.existsByMemberIdAndAlarmPurpose(memberId, request.alarmPurpose());
        if (exists) {
            throw ApplicationException.from(DUPLICATE_ALARM_PURPOSE);
        }

        // 1. 알람 엔티티 생성 및 저장
        AlarmEntity alarm = AlarmMapper.mapToAlarmEntity(request, memberEntity);
        alarmRepository.save(alarm);

        // 2. 다음 알람 발생 날짜 계산
        ZoneId memberZone = resolveMemberZone(memberId, deviceId);
        Set<DayOfWeek> repeatDays = alarm.getRepeatDays().stream()
            .map(Weekday::getDayOfWeek)
            .collect(Collectors.toSet());
        LocalDate nextDate = AlarmScheduleCalculator.getNextOccurrenceDate(
            repeatDays,
            ZonedDateTime.of(timeProvider.now(memberZone), memberZone),
            alarm.getTime()
        );
        LocalDateTime nextScheduledTime = AlarmScheduleCalculator.toDefaultZoneLocalDateTime(
            nextDate,
            alarm.getTime(),
            memberZone
        );

        // 3. 첫 알람 발생 내역 생성 및 저장
        AlarmOccurrenceEntity occurrence = AlarmMapper.mapToFirstAlarmOccurrenceEntity(
            alarm,
            nextDate,
            alarm.getTime(),
            nextScheduledTime
        );
        alarmOccurrenceRepository.save(occurrence);

        // 4. 알람의 다음 예정 시각 업데이트
        alarm.updateNextScheduledTime(nextScheduledTime);

        return AlarmMapper.mapToCreateAlarmResponse(alarm, occurrence);
    }

    @Override
    public CreateAlarmOccurrenceResponse createAlarmOccurrence(Long memberId, Long alarmId) {
        AlarmEntity alarmEntity = findAlarmById(alarmId);

        validAlarmOwner(memberId, alarmEntity.getMember().getId());

        // 각 알람이 울릴 때 알람 발생 내역은 1개만 허용(반복 울림은 alarm_ringing_log로 관리), 오늘 날짜 기준 알람 발생 내역이 이미 존재하면 예외 발생
        boolean alreadyExists = alarmOccurrenceRepository.existsByAlarmIdAndDate(alarmId, timeProvider.today());
        if (alreadyExists) {
            throw ApplicationException.from(ALREADY_OCCURRED_EXISTS);
        }

        AlarmOccurrenceEntity alarmOccurrenceEntity = AlarmMapper.mapToTodayFirstAlarmOccurrenceEntity(alarmEntity, timeProvider.today());
        alarmOccurrenceRepository.save(alarmOccurrenceEntity);

        return AlarmMapper.mapToCreateAlarmOccurrenceResponse(alarmOccurrenceEntity.getId());
    }

    @Override
    public void removeAlarmByAd(Long memberId, Long alarmId, AlarmDeleteByAdRequest request) {
        // 1. 알람을 조회하고 요청자가 알람 소유자인지 검증한다.
        AlarmEntity alarm = findAlarmById(alarmId);
        MemberEntity member = alarm.getMember();
        validAlarmOwner(memberId, member.getId());

        // 2. 오늘 회차가 아직 대기/울림 상태라면 결제 삭제 정책을 사용해야 한다.
        alarmOccurrenceRepository.findByAlarmIdAndDate(alarmId, timeProvider.today())
            .ifPresent(todayOccurrence -> {
                OccurrenceStatus status = todayOccurrence.getStatus();
                if (status == OccurrenceStatus.SCHEDULED || status == OccurrenceStatus.RINGING) {
                    throw ApplicationException.from(ALARM_DELETE_REQUIRES_PAYMENT);
                }
            });

        // 3. 오늘 회차가 없거나 이미 비활성화된 상태라면 알람을 소프트 삭제한다.
        LocalDateTime deletedAt = timeProvider.now();
        alarm.softDelete(deletedAt);

        // 4. 광고 증빙 토큰을 포함한 삭제 이력을 저장한다.
        alarmDeleteLogRepository.save(AlarmMapper.mapToAdDeleteLogEntity(
            alarm,
            member,
            request.adProofToken(),
            deletedAt,
            deletedAt
        ));

        // 5. 삭제 성공 시 별도 본문 없이 응답한다.
    }

    @Override
    public void removeAlarmByPayment(Long memberId, Long alarmId, AlarmDeleteByPaymentRequest request) {
        // 1. 알람을 조회하고 요청자가 알람 소유자인지 검증한다.
        AlarmEntity alarm = findAlarmById(alarmId);
        MemberEntity member = alarm.getMember();
        validAlarmOwner(memberId, member.getId());

        // 2. 오늘 알람 회차가 있어야 결제 삭제 대상이 된다.
        AlarmOccurrenceEntity todayOccurrence = alarmOccurrenceRepository
            .findByAlarmIdAndDate(alarmId, timeProvider.today())
            .orElseThrow(() -> ApplicationException.from(TODAY_IS_NOT_ALARM_DAY));

        // 3. 이미 비활성화된 회차는 광고 삭제 정책으로 넘긴다.
        if (todayOccurrence.getStatus() != OccurrenceStatus.SCHEDULED
            && todayOccurrence.getStatus() != OccurrenceStatus.RINGING) {
            throw ApplicationException.from(ALREADY_DEACTIVATED);
        }

        // 4. 같은 플랫폼 결제 ID가 이미 처리됐다면 재사용을 막는다.
        if (paymentRepository.existsByPaymentId(request.paymentId())) {
            throw ApplicationException.from(PaymentErrorCode.DUPLICATE_PAYMENT);
        }

        // 5. 요청 디바이스의 플랫폼에 맞는 결제 검증 클라이언트로 영수증을 검증한다.
        LocalDateTime processedAt = timeProvider.now();
        PaymentVerificationPort paymentClient = resolvePaymentClient(memberId, request.deviceId());
        boolean validPayment;
        String verificationFailReason = "";
        try {
            validPayment = paymentClient.verify(request.paymentId());
            if (!validPayment) {
                verificationFailReason = buildPaymentVerificationFailReason(
                    paymentClient,
                    request.paymentId(),
                    alarmId,
                    todayOccurrence.getId(),
                    "verification returned false"
                );
            }
        } catch (Exception e) {
            validPayment = false;
            verificationFailReason = buildPaymentVerificationFailReason(
                paymentClient,
                request.paymentId(),
                alarmId,
                todayOccurrence.getId(),
                e.getClass().getSimpleName() + ": " + e.getMessage()
            );
        }

        // 6. 검증 실패도 감사 추적을 위해 실패 결제와 삭제 실패 로그를 저장한 뒤 400으로 응답한다.
        if (!validPayment) {
            try {
                auditLogRecorder.recordPaymentDeleteFailure(
                    member,
                    alarm,
                    request.paymentId(),
                    processedAt,
                    verificationFailReason
                );
            } catch (DataIntegrityViolationException e) {
                throw ApplicationException.from(PaymentErrorCode.DUPLICATE_PAYMENT);
            }
            throw ApplicationException.from(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        // 7. 검증 성공 시 결제를 성공으로 기록하고 알람을 소프트 삭제한다.
        savePaymentOrThrowDuplicate(member, alarm, request.paymentId(), PaymentType.DELETE_ALARM);
        alarm.softDelete(processedAt);

        alarmDeleteLogRepository.save(AlarmMapper.mapToPaymentDeleteLogEntity(
            alarm,
            member,
            request.paymentId(),
            "",
            processedAt,
            processedAt
        ));

        // 8. consume은 외부 플랫폼 후처리이므로 DB 커밋이 성공한 뒤 실행한다.
        consumePaymentAfterCommit(paymentClient, request.paymentId());

        // 삭제 성공 시 별도 본문 없이 응답한다.
    }

    @Override
    public AlarmCheckinResponse checkinAlarm(Long memberId, Long alarmId, AlarmCheckinRequest request) {
        // 1. 알람과 회차를 조회하고 요청 사용자가 소유자인지 확인한다.
        AlarmEntity alarm = findAlarmById(alarmId);
        MemberEntity member = alarm.getMember();
        validAlarmOwner(memberId, member.getId());

        AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository
            .findByIdAndAlarmId(request.occurrenceId(), alarmId)
            .orElseThrow(() -> ApplicationException.from(ALARM_OCCURRENCE_NOT_FOUND));

        // 2. 이미 처리된 회차는 다시 체크인할 수 없다.
        if (occurrence.getStatus() != OccurrenceStatus.SCHEDULED
            && occurrence.getStatus() != OccurrenceStatus.RINGING) {
            throw ApplicationException.from(ALREADY_DEACTIVATED);
        }

        // 3. 위치 인증은 예정 시각 3시간 전부터만 허용한다.
        LocalDateTime processedAt = timeProvider.now();
        LocalDateTime checkinAvailableAt = occurrence.getScheduledAt().minusHours(3);
        if (processedAt.isBefore(checkinAvailableAt)) {
            throw ApplicationException.from(CHECKIN_NOT_YET_AVAILABLE);
        }

        // 4. 사용자가 알람 목적지 반경 50m 안에 있는지 검증한다.
        boolean isInRange = isWithinDistance(
            alarm.getLatitude(), alarm.getLongitude(),
            request.latitude(), request.longitude(), CHECKIN_RADIUS_METERS);

        if (!isInRange) {
            throw ApplicationException.from(CHECKIN_OUT_OF_RANGE);
        }

        // 5. 체크인 완료 시각을 기록하고 회차 상태를 CHECKIN으로 전환한다.
        occurrence.checkin(processedAt);

        // 6. 클라이언트 동기화를 위해 다음 예정 회차를 함께 조회한다.
        AlarmOccurrenceEntity nextOccurrence = alarmOccurrenceRepository
            .findNextScheduledByAlarmIds(List.of(alarmId), OccurrenceStatus.SCHEDULED, processedAt)
            .stream()
            .findFirst()
            .orElse(null);

        // 7. 체크인 로그는 본 처리와 분리해 커밋 후 별도 트랜잭션에서 best-effort로 저장한다.
        eventPublisher.publishEvent(new AlarmCheckinCompletedEvent(
            occurrence.getId(),
            member.getId(),
            request.deviceId(),
            request.latitude(),
            request.longitude(),
            processedAt,
            processedAt
        ));

        // 8. 다음 회차 정보를 포함한 응답을 반환한다.
        return AlarmMapper.mapToAlarmCheckinResponse(alarm, nextOccurrence);
    }

    @Override
    public AlarmPaymentResponse deactivateByPayment(Long memberId, Long alarmId, AlarmPaymentRequest request) {
        // 1. 알람을 조회하고 요청자가 알람 소유자인지 검증한다.
        AlarmEntity alarm = findAlarmById(alarmId);
        MemberEntity member = alarm.getMember();
        validAlarmOwner(memberId, member.getId());

        // 2. 요청한 알람 회차가 해당 알람에 속하는지 확인한다.
        AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository
            .findByIdAndAlarmId(request.occurrenceId(), alarmId)
            .orElseThrow(() -> ApplicationException.from(ALARM_OCCURRENCE_NOT_FOUND));

        // 3. 이미 처리된 회차는 중복 비활성화하지 않는다.
        if (occurrence.getStatus() != OccurrenceStatus.SCHEDULED
            && occurrence.getStatus() != OccurrenceStatus.RINGING) {
            throw ApplicationException.from(ALREADY_DEACTIVATED);
        }

        // 4. 결제 비활성화는 알람 예정 시각 3시간 전부터만 허용한다.
        LocalDateTime processedAt = timeProvider.now();
        LocalDateTime paymentAvailableAt = occurrence.getScheduledAt().minusHours(3);
        if (processedAt.isBefore(paymentAvailableAt)) {
            throw ApplicationException.from(PaymentErrorCode.PAYMENT_NOT_YET_AVAILABLE);
        }

        // 5. 같은 플랫폼 결제 ID가 이미 처리됐다면 재사용을 막는다.
        if (paymentRepository.existsByPaymentId(request.paymentId())) {
            throw ApplicationException.from(PaymentErrorCode.DUPLICATE_PAYMENT);
        }

        // 6. 요청 디바이스의 플랫폼에 맞는 결제 검증 클라이언트로 영수증을 검증한다.
        PaymentVerificationPort paymentClient = resolvePaymentClient(memberId, request.deviceId());
        boolean validPayment;
        String verificationFailReason = "";
        try {
            validPayment = paymentClient.verify(request.paymentId());
            if (!validPayment) {
                verificationFailReason = buildPaymentVerificationFailReason(
                    paymentClient,
                    request.paymentId(),
                    alarmId,
                    occurrence.getId(),
                    "verification returned false"
                );
            }
        } catch (Exception e) {
            validPayment = false;
            verificationFailReason = buildPaymentVerificationFailReason(
                paymentClient,
                request.paymentId(),
                alarmId,
                occurrence.getId(),
                e.getClass().getSimpleName() + ": " + e.getMessage()
            );
        }

        // 7. 검증 실패도 감사 추적을 위해 실패 결제와 상세 실패 로그를 저장한 뒤 400으로 응답한다.
        if (!validPayment) {
            try {
                auditLogRecorder.recordPaymentDeactivationFailure(
                    member,
                    alarm,
                    occurrence,
                    request.paymentId(),
                    request.deviceId(),
                    processedAt,
                    verificationFailReason
                );
            } catch (DataIntegrityViolationException e) {
                throw ApplicationException.from(PaymentErrorCode.DUPLICATE_PAYMENT);
            }
            throw ApplicationException.from(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        // 8. 검증 성공 시 결제를 성공으로 기록하고 알람 회차를 PAYMENT 상태로 비활성화한다.
        savePaymentOrThrowDuplicate(member, alarm, request.paymentId(), PaymentType.STOP_ALARM);
        occurrence.deactivateByPayment(processedAt);

        // 9. 클라이언트 동기화와 운영 추적을 위해 성공 로그를 남긴다.
        alarmDeactivationLogRepository.save(AlarmMapper.mapToPaymentDeactivationLogEntity(
            occurrence,
            member,
            request.paymentId(),
            request.deviceId(),
            processedAt,
            processedAt,
            DeactivationResult.SUCCESS,
            ""
        ));

        // 10. consume은 외부 플랫폼 후처리이므로 DB 커밋이 성공한 뒤 실행한다.
        consumePaymentAfterCommit(paymentClient, request.paymentId());

        // 11. 다음 예약 회차를 함께 내려 클라이언트가 로컬 알람을 재동기화하게 한다.
        AlarmOccurrenceEntity nextOccurrence = alarmOccurrenceRepository
            .findNextScheduledByAlarmIds(List.of(alarmId), OccurrenceStatus.SCHEDULED, processedAt)
            .stream()
            .findFirst()
            .orElse(null);

        return AlarmMapper.mapToAlarmPaymentResponse(alarm, processedAt, nextOccurrence);
    }

    @Override
    public void ringAlarm(Long memberId, Long alarmId, String deviceId) {
        AlarmEntity alarm = findAlarmById(alarmId);
        validAlarmOwner(memberId, alarm.getMember().getId());

        // 아직 처리되지 않은 알람 발생 이력 중 가장 최근 것을 가져옴.
        AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository
            .findTopByAlarmIdAndStatusInOrderByOccurrenceDateDescOccurrenceTimeDesc(
                alarmId,
                List.of(OccurrenceStatus.SCHEDULED, OccurrenceStatus.RINGING)
            )
            .orElseThrow(() -> ApplicationException.from(ALARM_OCCURRENCE_NOT_FOUND));

        // 아직 알람이 울릴 시간이 아니라면 예외 발생
        ZoneId memberZone = resolveMemberZone(memberId, deviceId);
        LocalDateTime now = timeProvider.now();
        if (!AlarmScheduleCalculator.isDue(
            occurrence.getOccurrenceDate(),
            occurrence.getOccurrenceTime(),
            memberZone,
            timeProvider.instant()
        )) {
            throw ApplicationException.from(NOT_ALARM_TIME);
        }

        // 알람 울림 로그 생성
        int ringIndex = occurrence.ring();
        AlarmRingingLogEntity log = AlarmMapper.mapToAlarmRingingLogEntity(
            occurrence,
            ringIndex,
            now
        );
        alarmRingingLogRepository.save(log);
    }

    @Override
    public void markReminderSent(Set<Long> occurrenceIds) {
        if (occurrenceIds == null || occurrenceIds.isEmpty()) return;
        // 벌크 업데이트가 가능하면 JPQL update 사용 권장 (락/동시성 고려)
        alarmOccurrenceRepository.markReminderSentIn(occurrenceIds);
    }

    /**
     * 위치 인증 반경 내 도착 여부 계산
     *
     * @param targetLat 기준 위도 (알람 설정 위치)
     * @param targetLon 기준 경도
     * @param reqLat 사용자 위도
     * @param reqLon 사용자 경도
     * @param radiusMeters 반경(m)
     * @return true if within radius
     */
    private boolean isWithinDistance(double targetLat, double targetLon, double reqLat, double reqLon, double radiusMeters) {
        double earthRadius = 6371000; // meters

        double dLat = Math.toRadians(reqLat - targetLat);
        double dLon = Math.toRadians(reqLon - targetLon);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(targetLat)) * Math.cos(Math.toRadians(reqLat))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = earthRadius * c;

        return distance <= radiusMeters;
    }

    private MemberEntity findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private AlarmEntity findAlarmById(Long alarmId) {
        return alarmRepository.findById(alarmId)
            .orElseThrow(() -> ApplicationException.from(ALARM_NOT_FOUND));
    }

    private PaymentVerificationPort resolvePaymentClient(Long memberId, String deviceId) {
        MemberDeviceEntity device = memberDeviceRepository.findByMember_IdAndDeviceId(memberId, deviceId)
            .orElseThrow(() -> ApplicationException.from(DeviceErrorCode.DEVICE_NOT_FOUND));

        String platform = device.getPlatform().toUpperCase(Locale.ROOT);

        return paymentVerificationPorts.stream()
            .filter(port -> port.supportedPlatform().equals(platform))
            .findFirst()
            .orElseThrow(() -> ApplicationException.from(CommonErrorCode.BAD_REQUEST));
    }

    private ZoneId resolveMemberZone(Long memberId, String deviceId) {
        return memberDeviceRepository.findByMember_IdAndDeviceId(memberId, deviceId)
            .map(MemberDeviceEntity::getTimeZone)
            .map(AlarmScheduleCalculator::resolveZone)
            .orElse(AlarmScheduleCalculator.DEFAULT_ZONE);
    }

    private String buildPaymentVerificationFailReason(
        PaymentVerificationPort paymentClient,
        String paymentId,
        Long alarmId,
        Long occurrenceId,
        String reason
    ) {
        return "PAYMENT_VERIFICATION_FAILED"
            + " platform=" + paymentClient.supportedPlatform()
            + ", paymentId=" + paymentId
            + ", alarmId=" + alarmId
            + ", occurrenceId=" + occurrenceId
            + ", reason=" + reason;
    }

    private void savePaymentOrThrowDuplicate(
        MemberEntity member,
        AlarmEntity alarm,
        String paymentId,
        PaymentType paymentType
    ) {
        try {
            paymentRepository.saveAndFlush(PaymentMapper.mapToPaymentEntity(
                member,
                alarm,
                paymentId,
                paymentType,
                PaymentStatus.SUCCESS
            ));
        } catch (DataIntegrityViolationException e) {
            throw ApplicationException.from(PaymentErrorCode.DUPLICATE_PAYMENT);
        }
    }

    private void consumePaymentAfterCommit(PaymentVerificationPort paymentClient, String paymentId) {
        Runnable consume = () -> {
            try {
                paymentClient.consume(paymentId);
            } catch (Exception e) {
                log.warn("Failed to consume payment. paymentId={}", paymentId, e);
            }
        };

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            consume.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                consume.run();
            }
        });
    }

    private static void validAlarmOwner(Long reqMemberId, Long alarmMemberId) {
        if (!reqMemberId.equals(alarmMemberId)) {
            throw ApplicationException.from(AuthErrorCode.PERMISSION_DENIED);
        }
    }

}
