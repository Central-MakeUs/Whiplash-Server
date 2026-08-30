package akuma.whiplash.domains.alarm.domain.service;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.*;

import akuma.whiplash.domains.ad.domain.constant.AdPurpose;
import akuma.whiplash.domains.ad.domain.service.AdSessionService;
import akuma.whiplash.domains.ad.persistence.entity.AdSessionEntity;
import akuma.whiplash.domains.ad.persistence.repository.AdSessionRepository;
import akuma.whiplash.domains.alarm.application.dto.request.*;
import akuma.whiplash.domains.alarm.application.dto.response.*;
import akuma.whiplash.domains.alarm.application.event.AlarmCheckinCompletedEvent;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.*;
import akuma.whiplash.domains.alarm.domain.util.AlarmScheduleCalculator;
import akuma.whiplash.domains.alarm.persistence.entity.*;
import akuma.whiplash.domains.alarm.persistence.repository.*;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.domains.member.persistence.entity.*;
import akuma.whiplash.domains.member.persistence.repository.*;
import akuma.whiplash.domains.payment.persistence.repository.PaymentRepository;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.util.date.TimeProvider;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AlarmCommandServiceImpl implements AlarmCommandService {
    private static final double CHECKIN_RADIUS_METERS = 50.0;
    private final AlarmRepository alarmRepository;
    private final AlarmOccurrenceRepository alarmOccurrenceRepository;
    private final AlarmRingingLogRepository alarmRingingLogRepository;
    private final AlarmDeactivationLogRepository alarmDeactivationLogRepository;
    private final AlarmDeleteLogRepository alarmDeleteLogRepository;
    private final AlarmOffLogRepository alarmOffLogRepository;
    private final AdSessionRepository adSessionRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final MemberDeviceRepository memberDeviceRepository;
    private final AdSessionService adSessionService;
    private final ApplicationEventPublisher eventPublisher;
    private final TimeProvider timeProvider;
    private final AlarmLocationCacheService alarmLocationCacheService;

    @Override
    public CreateAlarmResponse createAlarm(AlarmRegisterRequest request, Long memberId, String deviceId) {
        MemberEntity member = findMemberById(memberId);
        if (alarmRepository.existsByMemberIdAndAlarmPurpose(memberId, request.alarmPurpose())) {
            throw ApplicationException.from(DUPLICATE_ALARM_PURPOSE);
        }
        AlarmEntity alarm = AlarmMapper.mapToAlarmEntity(request, member, timeProvider.now());
        alarmRepository.save(alarm);
        ZoneId zone = resolveMemberZone(memberId, deviceId);
        Set<DayOfWeek> days = alarm.getRepeatDays().stream().map(Weekday::getDayOfWeek).collect(Collectors.toSet());
        LocalDate date = AlarmScheduleCalculator.getNextOccurrenceDate(days, ZonedDateTime.of(timeProvider.now(zone), zone), alarm.getTime());
        LocalDateTime scheduledAt = AlarmScheduleCalculator.toDefaultZoneLocalDateTime(date, alarm.getTime(), zone);
        AlarmOccurrenceEntity occurrence = AlarmMapper.mapToFirstAlarmOccurrenceEntity(alarm, date, alarm.getTime(), scheduledAt);
        alarmOccurrenceRepository.save(occurrence);
        alarm.updateNextScheduledTime(scheduledAt);
        return AlarmMapper.mapToCreateAlarmResponse(alarm, occurrence, zone);
    }

    @Override
    public AlarmAdSessionCreateResponse createOffAdSession(Long memberId, String deviceId, Long alarmId, AlarmOffAdSessionCreateRequest request) {
        LocalDateTime now = timeProvider.now();
        AlarmEntity alarm = findActiveAlarmByIdForUpdate(alarmId);
        validAlarmOwner(memberId, alarm.getMember().getId());
        AlarmOccurrenceEntity occurrence = findActiveOccurrenceForUpdate(request.occurrenceId(), alarmId, now);
        AdSessionEntity session = adSessionService.createSession(alarm.getMember(), alarm, occurrence, deviceId, AdPurpose.STOP_ALARM, now.plusMinutes(10));
        return AlarmMapper.mapToAlarmAdSessionCreateResponse(session);
    }

    @Override
    public AlarmDeactivationResponse deactivateByAd(Long memberId, String deviceId, Long alarmId, AlarmAdActionRequest request) {
        AlarmEntity alarm = findActiveAlarmByIdForUpdate(alarmId);
        validAlarmOwner(memberId, alarm.getMember().getId());
        AdSessionEntity target = adSessionService.getSessionTarget(request.adSessionId());
        Long occurrenceId = target.getAlarmOccurrence() == null ? null : target.getAlarmOccurrence().getId();
        if (occurrenceId == null) throw ApplicationException.from(akuma.whiplash.domains.ad.exception.AdErrorCode.AD_SESSION_MISMATCH);
        LocalDateTime now = timeProvider.now();
        AlarmOccurrenceEntity occurrence = findActiveOccurrenceForUpdate(occurrenceId, alarmId, now);
        AdSessionEntity session = adSessionService.getVerifiedSessionForConsume(request.adSessionId(), memberId, alarmId, deviceId, AdPurpose.STOP_ALARM, occurrence.getId());
        occurrence.deactivateByAd(now);
        alarmDeactivationLogRepository.save(AlarmMapper.mapToAdDeactivationLogEntity(occurrence, alarm.getMember(), request.adSessionId(), deviceId, now, now));
        session.consume(now);
        return AlarmMapper.mapToAlarmDeactivationResponse(alarm, now, findNextScheduledOccurrence(alarmId, now));
    }

    @Override
    public AlarmAdSessionCreateResponse createDeleteAdSession(Long memberId, String deviceId, Long alarmId) {
        AlarmEntity alarm = findActiveAlarmByIdForUpdate(alarmId);
        validAlarmOwner(memberId, alarm.getMember().getId());
        AdSessionEntity session = adSessionService.createSession(alarm.getMember(), alarm, null, deviceId, AdPurpose.DELETE_ALARM, timeProvider.now().plusMinutes(10));
        return AlarmMapper.mapToAlarmAdSessionCreateResponse(session);
    }

    @Override
    public void removeAlarmByAd(Long memberId, String deviceId, Long alarmId, AlarmAdActionRequest request) {
        AlarmEntity alarm = findActiveAlarmByIdForUpdate(alarmId);
        validAlarmOwner(memberId, alarm.getMember().getId());
        adSessionService.getVerifiedSessionForConsume(request.adSessionId(), memberId, alarmId, deviceId, AdPurpose.DELETE_ALARM, null);
        deleteAlarmAndRelatedData(alarmId);
    }

    @Override
    public AlarmCheckinResponse checkinAlarm(Long memberId, Long alarmId, AlarmCheckinRequest request) {
        AlarmEntity alarm = findAlarmById(alarmId);
        MemberEntity member = alarm.getMember();
        validAlarmOwner(memberId, member.getId());
        AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository.findByIdAndAlarmId(request.occurrenceId(), alarmId)
            .orElseThrow(() -> ApplicationException.from(ALARM_OCCURRENCE_NOT_FOUND));
        if (occurrence.getStatus() != OccurrenceStatus.SCHEDULED && occurrence.getStatus() != OccurrenceStatus.RINGING) throw ApplicationException.from(ALREADY_DEACTIVATED);
        LocalDateTime now = timeProvider.now();
        if (now.isBefore(occurrence.getScheduledAt().minusHours(3))) throw ApplicationException.from(CHECKIN_NOT_YET_AVAILABLE);
        if (alarm.getLocationSource() == LocationSource.GOOGLE_PLACE && !alarmLocationCacheService.hasValidGoogleLocationCache(alarm, now)) throw ApplicationException.from(ALARM_LOCATION_NOT_READY);
        if (!isWithinDistance(alarm.getLatitude(), alarm.getLongitude(), request.latitude(), request.longitude(), CHECKIN_RADIUS_METERS)) throw ApplicationException.from(CHECKIN_OUT_OF_RANGE);
        occurrence.checkin(now);
        eventPublisher.publishEvent(new AlarmCheckinCompletedEvent(occurrence.getId(), member.getId(), request.deviceId(), now, now));
        return AlarmMapper.mapToAlarmCheckinResponse(alarm, findNextScheduledOccurrence(alarmId, now));
    }

    @Override
    public void ringAlarm(Long memberId, Long alarmId, String deviceId) {
        AlarmEntity alarm = findAlarmById(alarmId);
        validAlarmOwner(memberId, alarm.getMember().getId());
        AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository.findTopByAlarmIdAndStatusInOrderByOccurrenceDateDescOccurrenceTimeDesc(alarmId, List.of(OccurrenceStatus.SCHEDULED, OccurrenceStatus.RINGING)).orElseThrow(() -> ApplicationException.from(ALARM_OCCURRENCE_NOT_FOUND));
        ZoneId zone = resolveMemberZone(memberId, deviceId);
        if (!AlarmScheduleCalculator.isDue(occurrence.getOccurrenceDate(), occurrence.getOccurrenceTime(), zone, timeProvider.instant())) throw ApplicationException.from(NOT_ALARM_TIME);
        alarmRingingLogRepository.save(AlarmMapper.mapToAlarmRingingLogEntity(occurrence, occurrence.ring(), timeProvider.now()));
    }

    @Override
    public void markReminderSent(Set<Long> occurrenceIds) {
        if (occurrenceIds != null && !occurrenceIds.isEmpty()) alarmOccurrenceRepository.markReminderSentIn(occurrenceIds);
    }

    private AlarmOccurrenceEntity findNextScheduledOccurrence(Long alarmId, LocalDateTime now) {
        return alarmOccurrenceRepository.findNextScheduledByAlarmIds(List.of(alarmId), OccurrenceStatus.SCHEDULED, now).stream().findFirst().orElse(null);
    }

    private void deleteAlarmAndRelatedData(Long alarmId) {
        adSessionRepository.deleteByAlarmId(alarmId);
        alarmRingingLogRepository.deleteByAlarmId(alarmId);
        alarmDeactivationLogRepository.deleteByAlarmId(alarmId);
        alarmOffLogRepository.deleteAllByAlarmId(alarmId);
        alarmDeleteLogRepository.deleteByAlarmId(alarmId);
        paymentRepository.deleteByAlarmId(alarmId);
        alarmOccurrenceRepository.deleteByAlarmId(alarmId);
        alarmRepository.deleteByAlarmId(alarmId);
    }
    private boolean isWithinDistance(double targetLat, double targetLon, double reqLat, double reqLon, double radiusMeters) {
        double dLat = Math.toRadians(reqLat - targetLat), dLon = Math.toRadians(reqLon - targetLon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(targetLat)) * Math.cos(Math.toRadians(reqLat)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6_371_000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) <= radiusMeters;
    }
    private MemberEntity findMemberById(Long memberId) { return memberRepository.findById(memberId).orElseThrow(() -> ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND)); }
    private AlarmEntity findAlarmById(Long alarmId) { return alarmRepository.findById(alarmId).orElseThrow(() -> ApplicationException.from(ALARM_NOT_FOUND)); }
    private AlarmEntity findActiveAlarmByIdForUpdate(Long alarmId) {
        AlarmEntity alarm = alarmRepository.findByIdWithMemberForUpdate(alarmId).orElseThrow(() -> ApplicationException.from(ALARM_NOT_FOUND));
        if (alarm.getStatus() == AlarmStatus.DELETED) throw ApplicationException.from(ALARM_NOT_FOUND);
        return alarm;
    }
    private AlarmOccurrenceEntity findActiveOccurrenceForUpdate(Long occurrenceId, Long alarmId, LocalDateTime now) {
        AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository.findByIdAndAlarmId(occurrenceId, alarmId).orElseThrow(() -> ApplicationException.from(ALARM_OCCURRENCE_NOT_FOUND));
        if (occurrence.getStatus() != OccurrenceStatus.SCHEDULED && occurrence.getStatus() != OccurrenceStatus.RINGING) throw ApplicationException.from(ALREADY_DEACTIVATED);
        if (now.isBefore(occurrence.getScheduledAt().minusHours(3))) throw ApplicationException.from(CHECKIN_NOT_YET_AVAILABLE);
        return occurrence;
    }
    private ZoneId resolveMemberZone(Long memberId, String deviceId) {
        return memberDeviceRepository.findByMember_IdAndDeviceId(memberId, deviceId).map(MemberDeviceEntity::getTimeZone).map(AlarmScheduleCalculator::resolveZone).orElse(AlarmScheduleCalculator.DEFAULT_ZONE);
    }
    private static void validAlarmOwner(Long memberId, Long alarmMemberId) { if (!memberId.equals(alarmMemberId)) throw ApplicationException.from(AuthErrorCode.PERMISSION_DENIED); }
}
