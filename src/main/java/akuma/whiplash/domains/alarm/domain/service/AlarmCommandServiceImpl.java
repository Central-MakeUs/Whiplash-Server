package akuma.whiplash.domains.alarm.domain.service;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.*;

import akuma.whiplash.domains.alarm.application.event.AlarmCheckinCompletedEvent;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmCheckinResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmPaymentResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.domain.constant.DeactivationResult;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmRingingLogEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmDeactivationLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOffLogRepository;
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
import akuma.whiplash.global.service.ArchiveService;
import akuma.whiplash.global.util.date.DateUtil;
import akuma.whiplash.global.util.date.TimeProvider;
import akuma.whiplash.infrastructure.payment.PaymentVerificationPort;
import akuma.whiplash.infrastructure.redis.RingingAlarmRedisRepository;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AlarmCommandServiceImpl implements AlarmCommandService {

    private final AlarmRepository alarmRepository;
    private final AlarmOccurrenceRepository alarmOccurrenceRepository;
    private final AlarmOffLogRepository alarmOffLogRepository;
    private final AlarmRingingLogRepository alarmRingingLogRepository;
    private final AlarmDeactivationLogRepository alarmDeactivationLogRepository;
    private final MemberRepository memberRepository;
    private final MemberDeviceRepository memberDeviceRepository;
    private final PaymentRepository paymentRepository;
    private final List<PaymentVerificationPort> paymentVerificationPorts;
    private final ArchiveService archiveService;
    private final RingingAlarmRedisRepository ringingAlarmRedisRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TimeProvider timeProvider;

    @Value("${oauth.google.sheet.id}")
    private String spreadsheetsId;

    @Value("${oauth.google.sheet.credentials-path}")
    private String credentialsPath;

    @Value("${oauth.google.sheet.range}")
    private String sheetRange;

    private static final double CHECKIN_RADIUS_METERS = 50.0;

    @Override
    public CreateAlarmResponse createAlarm(AlarmRegisterRequest request, Long memberId) {
        MemberEntity memberEntity = findMemberById(memberId);

        boolean exists = alarmRepository.existsByMemberIdAndAlarmPurpose(memberId, request.alarmPurpose());
        if (exists) {
            throw ApplicationException.from(DUPLICATE_ALARM_PURPOSE);
        }

        // 1. 알람 엔티티 생성 및 저장
        AlarmEntity alarm = AlarmMapper.mapToAlarmEntity(request, memberEntity);
        alarmRepository.save(alarm);

        // 2. 다음 알람 발생 날짜 계산
        Set<DayOfWeek> repeatDays = alarm.getRepeatDays().stream()
            .map(Weekday::getDayOfWeek)
            .collect(Collectors.toSet());
        
        // 현재 시각 기준으로 가장 가까운 발생일 계산 (오늘 포함 여부는 getNextOccurrenceDate 내부 로직 따름)
        LocalDate nextDate = DateUtil.getNextOccurrenceDate(repeatDays, timeProvider.now(), alarm.getTime());
        LocalDateTime nextScheduledTime = LocalDateTime.of(nextDate, alarm.getTime());

        // 3. 첫 알람 발생 내역 생성 및 저장
        AlarmOccurrenceEntity occurrence = AlarmMapper.mapToFirstAlarmOccurrenceEntity(alarm, nextDate, alarm.getTime());
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

        AlarmOccurrenceEntity alarmOccurrenceEntity = AlarmMapper.mapToTodayFirstAlarmOccurrenceEntity(alarmEntity);
        alarmOccurrenceRepository.save(alarmOccurrenceEntity);

        return AlarmMapper.mapToCreateAlarmOccurrenceResponse(alarmOccurrenceEntity.getId());
    }

    @Override
    public void removeAlarm(Long memberId, Long alarmId, String reason) {
        // 1-1. 알람 조회 및 소유자 검증
        AlarmEntity alarm = findAlarmById(alarmId);
        validAlarmOwner(memberId, alarm.getMember().getId());

        // 1-2. 울리고 있을 수 있으므로 Redis Sorted Set에서 먼저 제거
        ringingAlarmRedisRepository.remove(alarmId, memberId);

        // 1-3. 삭제할 데이터 삭제 전 아카이빙
        archiveService.archiveAlarmWithRelations(alarmId);

        // 2. 삭제 사유를 Google Sheets에 로그로 기록
        logDeleteReason(alarm.getAlarmPurpose(), reason);

        // 3. 알람 발생 내역 전체 조회 및 관련 로그 제거
        List<AlarmOccurrenceEntity> occurrences = alarmOccurrenceRepository.findAllByAlarmId(alarmId);
        for (AlarmOccurrenceEntity occ : occurrences) {
            alarmRingingLogRepository.deleteAllByAlarmOccurrenceId(occ.getId());
        }

        // 4. 알람 발생 이력, 끈 이력 삭제
        alarmOccurrenceRepository.deleteAll(occurrences);
        alarmOffLogRepository.deleteAllByAlarmId(alarmId);

        // 5. 알람 자체 삭제
        alarmRepository.delete(alarm);
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

        // 6. 더 이상 울리는 알람이 아니므로 캐시에서 제거하고 알람 revision을 증가시킨다.
        ringingAlarmRedisRepository.remove(alarmId, memberId);

        alarm.incrementRevision();

        // 7. 클라이언트 동기화를 위해 다음 예정 회차를 함께 조회한다.
        AlarmOccurrenceEntity nextOccurrence = alarmOccurrenceRepository
            .findNextScheduledByAlarmIds(List.of(alarmId), OccurrenceStatus.SCHEDULED, processedAt)
            .stream()
            .findFirst()
            .orElse(null);

        // 8. 체크인 로그는 본 처리와 분리해 커밋 후 별도 트랜잭션에서 best-effort로 저장한다.
        eventPublisher.publishEvent(new AlarmCheckinCompletedEvent(
            occurrence.getId(),
            member.getId(),
            request.deviceId(),
            request.latitude(),
            request.longitude(),
            processedAt,
            processedAt
        ));

        // 9. revision과 다음 회차 정보를 포함한 응답을 반환한다.
        return AlarmMapper.mapToAlarmCheckinResponse(alarm, nextOccurrence);
    }

    @Override
    // 결제 검증 실패도 payment(FAILED)와 비활성화 실패 로그를 남겨야 하므로 ApplicationException으로 롤백하지 않는다.
    @Transactional(noRollbackFor = ApplicationException.class)
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
            paymentRepository.save(PaymentMapper.mapToPaymentEntity(
                member,
                alarm,
                request.paymentId(),
                PaymentType.STOP_ALARM,
                PaymentStatus.FAILED
            ));

            alarmDeactivationLogRepository.save(AlarmMapper.mapToPaymentDeactivationLogEntity(
                occurrence,
                member,
                request.paymentId(),
                request.deviceId(),
                processedAt,
                processedAt,
                DeactivationResult.FAIL,
                verificationFailReason
            ));
            throw ApplicationException.from(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        // 8. 검증 성공 시 결제를 성공으로 기록하고 알람 회차를 PAYMENT 상태로 비활성화한다.
        paymentRepository.save(PaymentMapper.mapToPaymentEntity(
            member,
            alarm,
            request.paymentId(),
            PaymentType.STOP_ALARM,
            PaymentStatus.SUCCESS
        ));
        occurrence.deactivateByPayment(processedAt);
        alarm.incrementRevision();
        ringingAlarmRedisRepository.remove(alarmId, memberId);

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

        // 10. consume은 외부 플랫폼 후처리이므로 실패해도 알람 비활성화 트랜잭션은 유지한다.
        try {
            paymentClient.consume(request.paymentId());
        } catch (Exception e) {
            log.warn("Failed to consume payment. paymentId={}", request.paymentId(), e);
        }

        // 11. 변경된 revision과 다음 예약 회차를 함께 내려 클라이언트가 로컬 알람을 재동기화하게 한다.
        AlarmOccurrenceEntity nextOccurrence = alarmOccurrenceRepository
            .findNextScheduledByAlarmIds(List.of(alarmId), OccurrenceStatus.SCHEDULED, processedAt)
            .stream()
            .findFirst()
            .orElse(null);

        return AlarmMapper.mapToAlarmPaymentResponse(alarm, processedAt, nextOccurrence);
    }

    @Override
    public void ringAlarm(Long memberId, Long alarmId) {
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
        LocalDateTime now = timeProvider.now();
        LocalDateTime scheduledDateTime = LocalDateTime.of(occurrence.getOccurrenceDate(), occurrence.getOccurrenceTime());
        if (now.isBefore(scheduledDateTime)) {
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

        // Redis Sorted Set 적재: score = 알람 예정 시각 epoch millis
        // 동일 member로 재호출 시 score만 갱신되므로 멱등하다.
        long score = scheduledDateTime
            .atZone(ZoneId.of("Asia/Seoul"))
            .toInstant()
            .toEpochMilli();
        ringingAlarmRedisRepository.add(alarmId, memberId, score);
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

    private void logDeleteReason(String alarmPurpose, String reason) {
        try {
            // 1. Google Sheets API 클라이언트 생성
            Sheets sheets = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(
                    ServiceAccountCredentials.fromStream(
                        new ClassPathResource(credentialsPath).getInputStream() // classpath 리소스 처리
                    ).createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS))
                )
            )
                .setApplicationName("눈 떠!")
                .build();

            // 2. 기록할 값 구성
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = timeProvider.now().format(formatter);

            ValueRange body = new ValueRange().setValues(
                List.of(List.of(alarmPurpose, reason, formattedDateTime))
            );

            // 3. 스프레드시트에 행 추가 (append 방식)
            sheets.spreadsheets().values()
                .append(spreadsheetsId, sheetRange, body)
                .setValueInputOption("RAW")
                .execute();

        } catch (Exception e) {
            log.warn("Failed to log delete reason", e);
        }
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

    private static void validAlarmOwner(Long reqMemberId, Long alarmMemberId) {
        if (!reqMemberId.equals(alarmMemberId)) {
            throw ApplicationException.from(AuthErrorCode.PERMISSION_DENIED);
        }
    }

}
