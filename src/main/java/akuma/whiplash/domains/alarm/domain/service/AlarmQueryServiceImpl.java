package akuma.whiplash.domains.alarm.domain.service;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.ALARM_NOT_FOUND;

import akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo;
import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushInfo;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmDeleteMethodResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmSyncResponse;
import akuma.whiplash.domains.alarm.application.dto.response.GetAlarmsResponse;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.AlarmDeleteMethod;
import akuma.whiplash.domains.alarm.domain.constant.AlarmStatus;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.domain.util.AlarmScheduleCalculator;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberDeviceEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.util.date.DateUtil;
import akuma.whiplash.global.util.date.TimeProvider;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmQueryServiceImpl implements AlarmQueryService {

    private static final List<OccurrenceStatus> PROCESSED_STATUSES = List.of(
        OccurrenceStatus.CHECKIN,
        OccurrenceStatus.WATCH_AD,
        OccurrenceStatus.PAYMENT,
        OccurrenceStatus.CANCELED,
        OccurrenceStatus.MISSED
    );

    private final AlarmRepository alarmRepository;
    private final AlarmOccurrenceRepository alarmOccurrenceRepository;
    private final MemberRepository memberRepository;
    private final MemberDeviceRepository memberDeviceRepository;
    private final TimeProvider timeProvider;

    @Override
    public GetAlarmsResponse getAlarms(Long memberId, String deviceId) {
        memberRepository.findById(memberId)
            .orElseThrow(() -> ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        ZoneId memberZone = resolveMemberZone(memberId, deviceId);
        List<AlarmEntity> alarms = alarmRepository.findAllByMemberIdAndStatusNot(memberId, AlarmStatus.DELETED);

        if (alarms.isEmpty()) {
            return GetAlarmsResponse.builder()
                .timeZone(memberZone.getId())
                .alarms(List.of())
                .build();
        }

        LocalDate today = timeProvider.today(memberZone);
        LocalDateTime now = timeProvider.now(memberZone);
        List<Long> alarmIds = alarms.stream().map(AlarmEntity::getId).toList();

        // 1. 최근 처리 완료 회차 벌크 조회 (N+1 제거)
        Map<Long, AlarmOccurrenceEntity> latestProcessedMap =
            alarmOccurrenceRepository
                .findLatestProcessedByAlarmIds(alarmIds, PROCESSED_STATUSES)
                .stream()
                .collect(Collectors.toMap(
                    ao -> ao.getAlarm().getId(),
                    ao -> ao,
                    (a, b) -> a
                ));

        // 2. 각 알람의 first / second / third 날짜 계산
        record AlarmDates(LocalDate first, LocalDate second, LocalDate third) {}

        Map<Long, AlarmDates> alarmDatesMap = alarms.stream().collect(
            Collectors.toMap(
                AlarmEntity::getId,
                alarm -> {
                    Set<DayOfWeek> days = alarm.getRepeatDays().stream()
                        .map(Weekday::getDayOfWeek)
                        .collect(Collectors.toSet());
                    LocalDate first = DateUtil.getNextOccurrenceDate(days, today);
                    LocalDate second = DateUtil.getNextOccurrenceDate(days, first.plusDays(1));
                    LocalDate third = DateUtil.getNextOccurrenceDate(days, second.plusDays(1));
                    return new AlarmDates(first, second, third);
                }
            )
        );

        // 3. occurrenceId 벌크 조회 (다음다음다음 회차까지 포함: 현재 회차 처리 시 nextNext가 third로 밀릴 수 있음)
        List<LocalDate> targetDates = alarmDatesMap.values().stream()
            .flatMap(d -> Stream.of(d.first(), d.second(), d.third()))
            .distinct()
            .toList();

        Map<Long, Map<LocalDate, Long>> occurrenceIdMap = alarmOccurrenceRepository
            .findByAlarmIdsAndOccurrenceDates(alarmIds, targetDates)
            .stream()
            .collect(Collectors.groupingBy(
                ao -> ao.getAlarm().getId(),
                Collectors.toMap(
                    AlarmOccurrenceEntity::getOccurrenceDate,
                    AlarmOccurrenceEntity::getId
                )
            ));

        // 4. 응답 DTO 생성
        return GetAlarmsResponse.builder()
            .alarms(alarms.stream()
                .map(alarm -> AlarmMapper.mapToAlarmPreviewDto(
                    alarm,
                    now,
                    latestProcessedMap.get(alarm.getId()),
                    alarmDatesMap.get(alarm.getId()).first(),
                    alarmDatesMap.get(alarm.getId()).second(),
                    alarmDatesMap.get(alarm.getId()).third(),
                    occurrenceIdMap.getOrDefault(alarm.getId(), Map.of()),
                    memberZone
                ))
                .toList())
            .timeZone(memberZone.getId())
            .build();
    }

    @Override
    public AlarmSyncResponse getSyncAlarms(Long memberId) {
        // 동기화는 회원 기준 데이터이므로, 먼저 유효한 회원인지 확인한다.
        memberRepository.findById(memberId)
            .orElseThrow(() -> ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        // 삭제된 알람은 클라이언트 로컬 예약 대상이 아니므로 제외한다.
        List<AlarmEntity> alarms = alarmRepository.findAllByMemberIdAndStatusNot(memberId, AlarmStatus.DELETED);
        LocalDateTime now = LocalDateTime.now();

        if (alarms.isEmpty()) {
            return AlarmMapper.mapToSyncResponse(now, List.of());
        }

        // 각 알람의 다음 예정 회차를 한 번에 조회해 N+1 쿼리를 방지한다.
        List<Long> alarmIds = alarms.stream().map(AlarmEntity::getId).toList();
        Map<Long, AlarmOccurrenceEntity> nextOccurrenceMap = alarmOccurrenceRepository
            .findNextScheduledByAlarmIds(alarmIds, OccurrenceStatus.SCHEDULED, now)
            .stream()
            .collect(Collectors.toMap(
                ao -> ao.getAlarm().getId(),
                ao -> ao,
                (a, b) -> a
            ));

        // 다음 회차가 없는 알람은 nextOccurrence=null로 내려 클라이언트가 예약을 생략하게 한다.
        return AlarmMapper.mapToSyncResponse(
            now,
            alarms.stream()
                .map(alarm -> AlarmMapper.mapToSyncItem(alarm, nextOccurrenceMap.get(alarm.getId())))
                .toList()
        );
    }

    @Override
    public AlarmDeleteMethodResponse getAlarmDeleteMethod(Long memberId, Long alarmId) {
        AlarmEntity alarm = findAlarmById(alarmId);
        validAlarmOwner(memberId, alarm.getMember().getId());

        AlarmDeleteMethod deleteMethod = alarmOccurrenceRepository
            .findStatusByAlarmIdAndDate(alarmId, timeProvider.today())
            .map(status -> requiresPayment(status)
                ? AlarmDeleteMethod.PAYMENT
                : AlarmDeleteMethod.AD)
            .orElse(AlarmDeleteMethod.AD);

        return AlarmMapper.mapToAlarmDeleteMethodResponse(deleteMethod);
    }

    @Override
    public List<OccurrencePushInfo> getPreNotificationTargets(LocalDateTime startInclusive, LocalDateTime endInclusive) {
        return alarmOccurrenceRepository.findPreNotificationTargetsByScheduledAtBetween(
            startInclusive,
            endInclusive,
            OccurrenceStatus.SCHEDULED
        );
    }

    @Override
    public List<RingingPushInfo> getRingingNotificationTargets() {
        return alarmOccurrenceRepository.findRingingNotificationTargets(OccurrenceStatus.RINGING);
    }

    private AlarmEntity findAlarmById(Long alarmId) {
        return alarmRepository.findByIdWithMember(alarmId)
            .orElseThrow(() -> ApplicationException.from(ALARM_NOT_FOUND));
    }

    private boolean requiresPayment(OccurrenceStatus status) {
        return status == OccurrenceStatus.SCHEDULED || status == OccurrenceStatus.RINGING;
    }

    private static void validAlarmOwner(Long reqMemberId, Long alarmMemberId) {
        if (!reqMemberId.equals(alarmMemberId)) {
            throw ApplicationException.from(AuthErrorCode.PERMISSION_DENIED);
        }
    }

    private ZoneId resolveMemberZone(Long memberId, String deviceId) {
        return memberDeviceRepository.findByMember_IdAndDeviceId(memberId, deviceId)
            .map(MemberDeviceEntity::getTimeZone)
            .map(AlarmScheduleCalculator::resolveZone)
            .orElse(AlarmScheduleCalculator.DEFAULT_ZONE);
    }
}
