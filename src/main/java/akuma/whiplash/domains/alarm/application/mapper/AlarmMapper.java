package akuma.whiplash.domains.alarm.application.mapper;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmCheckinResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmDeleteMethodResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmPaymentResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmAdSessionCreateResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmSyncItemDto;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmSyncResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import akuma.whiplash.domains.alarm.application.dto.response.NextOccurrenceResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmPreviewDto;
import akuma.whiplash.domains.alarm.domain.constant.AlarmDeleteMethod;
import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.domain.constant.DeactivationResult;
import akuma.whiplash.domains.alarm.domain.constant.AlarmStatus;
import akuma.whiplash.domains.alarm.domain.constant.DeleteType;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.domain.constant.SoundType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.domain.util.AlarmScheduleCalculator;
import akuma.whiplash.domains.alarm.exception.AlarmErrorCode;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmDeleteLogEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmDeactivationLogEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.ad.persistence.entity.AdSessionEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmRingingLogEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.global.exception.ApplicationException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AlarmMapper {

    private AlarmMapper() {
        throw new IllegalArgumentException();
    }

    public static AlarmEntity mapToAlarmEntity(AlarmRegisterRequest request, MemberEntity memberEntity) {
        return AlarmEntity.builder()
            .member(memberEntity)
            .alarmPurpose(request.alarmPurpose())
            .time(request.alarmTime())
            .repeatDays(mapToWeekdays(request.repeatDays()))
            .soundType(SoundType.from(request.soundType()))
            .latitude(request.place().latitude())
            .longitude(request.place().longitude())
            .address(request.place().address())
            .status(AlarmStatus.ACTIVE)
            .build();
    }

    public static AlarmOccurrenceEntity mapToFirstAlarmOccurrenceEntity(AlarmEntity alarm, LocalDate nextDate, LocalTime alarmTime) {
        return mapToFirstAlarmOccurrenceEntity(alarm, nextDate, alarmTime, LocalDateTime.of(nextDate, alarmTime));
    }

    public static AlarmOccurrenceEntity mapToFirstAlarmOccurrenceEntity(
        AlarmEntity alarm,
        LocalDate nextDate,
        LocalTime alarmTime,
        LocalDateTime scheduledAt
    ) {
        return AlarmOccurrenceEntity.builder()
            .alarm(alarm)
            .occurrenceDate(nextDate)
            .occurrenceTime(alarmTime)
            .scheduledAt(scheduledAt)
            .status(OccurrenceStatus.SCHEDULED)
            .alarmRinging(false)
            .ringingCount(0)
            .reminderSent(false)
            .build();
    }

    public static CreateAlarmResponse mapToCreateAlarmResponse(
        AlarmEntity alarm,
        AlarmOccurrenceEntity occurrence,
        ZoneId memberZone
    ) {
        return CreateAlarmResponse.builder()
            .alarmId(alarm.getId())
            .timeZone(memberZone.getId())
            .nextOccurrence(NextOccurrenceResponse.builder()
                .occurrenceId(occurrence.getId())
                .scheduledDate(occurrence.getOccurrenceDate())
                .scheduledTime(occurrence.getOccurrenceTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .dayOfWeek(Weekday.getDescriptionOfDayOfWeek(occurrence.getOccurrenceDate().getDayOfWeek()))
                .scheduledAtUtc(AlarmScheduleCalculator.toInstant(
                    occurrence.getOccurrenceDate(),
                    occurrence.getOccurrenceTime(),
                    memberZone
                ).toString())
                .build())
            .build();
    }

    public static AlarmOccurrenceEntity mapToTodayFirstAlarmOccurrenceEntity(AlarmEntity alarmEntity, LocalDate today) {
        DayOfWeek todayDayOfWeek = today.getDayOfWeek();

        boolean isTodayAlarmDay = alarmEntity.getRepeatDays().stream()
            .anyMatch(weekday -> weekday.getDayOfWeek() == todayDayOfWeek);

        if (!isTodayAlarmDay) {
            throw ApplicationException.from(AlarmErrorCode.TODAY_IS_NOT_ALARM_DAY); // 오늘은 울릴 날이 아님
        }

        return AlarmOccurrenceEntity.builder()
            .alarm(alarmEntity)
            .occurrenceDate(today)
            .occurrenceTime(alarmEntity.getTime())
            .scheduledAt(LocalDateTime.of(today, alarmEntity.getTime()))
            .status(OccurrenceStatus.RINGING)
            .checkinTime(null)
            .alarmRinging(true)
            .deactivatedAt(null)
            .ringingCount(1)
            .reminderSent(false)
            .build();
    }

    public static AlarmOccurrenceEntity mapToAlarmOccurrenceForDate(AlarmEntity alarm, LocalDate date) {
        return mapToAlarmOccurrenceForDate(alarm, alarm.getTime(), date);
    }

    public static AlarmOccurrenceEntity mapToAlarmOccurrenceForDate(AlarmEntity alarm, LocalTime alarmTime, LocalDate date) {
        return mapToAlarmOccurrenceForDate(alarm, alarmTime, date, LocalDateTime.of(date, alarmTime));
    }

    public static AlarmOccurrenceEntity mapToAlarmOccurrenceForDate(
        AlarmEntity alarm,
        LocalTime alarmTime,
        LocalDate date,
        LocalDateTime scheduledAt
    ) {
        return AlarmOccurrenceEntity.builder()
            .alarm(alarm)
            .occurrenceDate(date)
            .occurrenceTime(alarmTime)
            .scheduledAt(scheduledAt)
            .status(OccurrenceStatus.SCHEDULED)
            .deactivatedAt(null)
            .checkinTime(null)
            .alarmRinging(false)
            .ringingCount(0)
            .reminderSent(false)
            .build();
    }

    public static AlarmRingingLogEntity mapToAlarmRingingLogEntity(
        AlarmOccurrenceEntity occurrence,
        int ringIndex,
        LocalDateTime ringedAt
    ) {
        return AlarmRingingLogEntity.builder()
            .alarmOccurrence(occurrence)
            .ringIndex(ringIndex)
            .ringedAt(ringedAt)
            .build();
    }

    public static CreateAlarmOccurrenceResponse mapToCreateAlarmOccurrenceResponse(Long occurrenceId) {
        return CreateAlarmOccurrenceResponse.builder()
            .occurrenceId(occurrenceId)
            .build();
    }

    public static AlarmDeactivationLogEntity mapToAlarmDeactivationLogEntity(
        AlarmOccurrenceEntity occurrence,
        MemberEntity member,
        String deviceId,
        LocalDateTime requestedAt,
        LocalDateTime processedAt
    ) {
        return AlarmDeactivationLogEntity.builder()
            .alarmOccurrence(occurrence)
            .member(member)
            .paymentId(null)
            .deactivateType(DeactivateType.CHECKIN)
            .requestDeviceId(deviceId)
            .requestedAt(requestedAt)
            .processedAt(processedAt)
            .result(DeactivationResult.SUCCESS)
            .failReason("")
            .build();
    }

    public static AlarmCheckinResponse mapToAlarmCheckinResponse(
        AlarmEntity alarm,
        AlarmOccurrenceEntity nextOccurrence
    ) {
        return AlarmCheckinResponse.builder()
            .alarmId(alarm.getId())
            .nextOccurrence(nextOccurrence == null ? null : AlarmCheckinResponse.NextOccurrenceInfo.builder()
                .occurrenceId(nextOccurrence.getId())
                .scheduledAt(nextOccurrence.getScheduledAt())
                .build())
            .build();
    }

    public static AlarmPaymentResponse mapToAlarmPaymentResponse(
        AlarmEntity alarm,
        LocalDateTime deactivatedAt,
        AlarmOccurrenceEntity nextOccurrence
    ) {
        return AlarmPaymentResponse.builder()
            .alarmId(alarm.getId())
            .deactivatedAt(deactivatedAt)
            .nextOccurrence(nextOccurrence == null ? null : AlarmPaymentResponse.NextOccurrenceInfo.builder()
                .occurrenceId(nextOccurrence.getId())
                .scheduledAt(nextOccurrence.getScheduledAt())
                .build())
            .build();
    }

    public static AlarmDeactivationLogEntity mapToPaymentDeactivationLogEntity(
        AlarmOccurrenceEntity occurrence,
        MemberEntity member,
        String paymentId,
        String deviceId,
        LocalDateTime requestedAt,
        LocalDateTime processedAt,
        DeactivationResult result,
        String failReason
    ) {
        return AlarmDeactivationLogEntity.builder()
            .alarmOccurrence(occurrence)
            .member(member)
            .paymentId(paymentId)
            .deactivateType(DeactivateType.PAYMENT)
            .requestDeviceId(deviceId)
            .requestedAt(requestedAt)
            .processedAt(processedAt)
            .result(result)
            .failReason(failReason)
            .build();
    }

    public static AlarmDeleteLogEntity mapToPaymentDeleteLogEntity(
        AlarmEntity alarm,
        MemberEntity member,
        String paymentId,
        String reason,
        LocalDateTime requestedAt,
        LocalDateTime deletedAt
    ) {
        return AlarmDeleteLogEntity.builder()
            .alarm(alarm)
            .member(member)
            .deleteType(DeleteType.PAYMENT)
            .reason(reason)
            .paymentId(paymentId)
            .requestedAt(requestedAt)
            .deletedAt(deletedAt)
            .build();
    }

    public static AlarmDeleteLogEntity mapToPaymentDeleteFailureLogEntity(
        AlarmEntity alarm,
        MemberEntity member,
        String paymentId,
        String reason,
        LocalDateTime requestedAt
    ) {
        return AlarmDeleteLogEntity.builder()
            .alarm(alarm)
            .member(member)
            .deleteType(DeleteType.PAYMENT_FAILED)
            .reason(truncateReason(reason))
            .paymentId(paymentId)
            .requestedAt(requestedAt)
            .deletedAt(null)
            .build();
    }

    public static AlarmDeleteLogEntity mapToAdDeleteLogEntity(
        AlarmEntity alarm,
        MemberEntity member,
        String adSessionId,
        LocalDateTime requestedAt,
        LocalDateTime deletedAt
    ) {
        return AlarmDeleteLogEntity.builder()
            .alarm(alarm)
            .member(member)
            .deleteType(DeleteType.AD)
            .reason("")
            .adProofToken(adSessionId)
            .requestedAt(requestedAt)
            .deletedAt(deletedAt)
            .build();
    }

    public static AlarmAdSessionCreateResponse mapToAlarmAdSessionCreateResponse(AdSessionEntity adSession) {
        return AlarmAdSessionCreateResponse.builder()
            .adSessionId(adSession.getAdSessionId())
            .expiresAt(adSession.getExpiresAt())
            .build();
    }

    public static AlarmPreviewDto mapToAlarmPreviewDto(
        AlarmEntity alarm,
        LocalDateTime now,
        AlarmOccurrenceEntity latestProcessedOccurrence,
        LocalDate firstDate,
        LocalDate secondDate,
        LocalDate thirdDate,
        Map<LocalDate, Long> occurrenceIdsByDate,
        ZoneId memberZone
    ) {
        boolean isCurrentOccurrenceProcessed = latestProcessedOccurrence != null
            && latestProcessedOccurrence.getOccurrenceDate().isEqual(firstDate);

        String status = mapToStatusLabel(alarm.getStatus());

        LocalDate resolvedNext     = isCurrentOccurrenceProcessed ? secondDate : firstDate;
        LocalDate resolvedNextNext = isCurrentOccurrenceProcessed ? thirdDate : secondDate;

        boolean arrivalCheckEnabled = false;
        if (alarm.getStatus() != AlarmStatus.INACTIVE) {
            LocalDateTime alarmDateTime = LocalDateTime.of(resolvedNext, alarm.getTime());
            arrivalCheckEnabled = !now.isBefore(alarmDateTime.minusHours(3));
        }

        return AlarmPreviewDto.builder()
            .alarmId(alarm.getId())
            .alarmPurpose(alarm.getAlarmPurpose())
            .alarmTime(alarm.getTime().format(DateTimeFormatter.ofPattern("HH:mm")))
            .repeatDays(alarm.getRepeatDays().stream().map(Weekday::getDescription).toList())
            .address(alarm.getAddress())
            .status(status)
            .arrivalCheckEnabled(arrivalCheckEnabled)
            .nextOccurrence(mapToOccurrenceInfo(alarm, resolvedNext, occurrenceIdsByDate, memberZone))
            .nextNextOccurrence(mapToOccurrenceInfo(alarm, resolvedNextNext, occurrenceIdsByDate, memberZone))
            .build();
    }

    private static AlarmPreviewDto.OccurrenceInfo mapToOccurrenceInfo(
        AlarmEntity alarm,
        LocalDate occurrenceDate,
        Map<LocalDate, Long> occurrenceIdsByDate,
        ZoneId memberZone
    ) {
        return AlarmPreviewDto.OccurrenceInfo.builder()
            .occurrenceId(occurrenceIdsByDate.get(occurrenceDate))
            .scheduledDate(occurrenceDate)
            .scheduledTime(alarm.getTime().format(DateTimeFormatter.ofPattern("HH:mm")))
            .dayOfWeek(Weekday.getDescriptionOfDayOfWeek(occurrenceDate.getDayOfWeek()))
            .scheduledAtUtc(AlarmScheduleCalculator.toInstant(
                occurrenceDate,
                alarm.getTime(),
                memberZone
            ).toString())
            .build();
    }

    public static AlarmSyncItemDto mapToSyncItem(
        AlarmEntity alarm,
        AlarmOccurrenceEntity nextOccurrence,
        ZoneId memberZone
    ) {
        return AlarmSyncItemDto.builder()
            .alarmId(alarm.getId())
            .status(mapToStatusLabel(alarm.getStatus()))
            .nextOccurrence(nextOccurrence == null ? null : AlarmSyncItemDto.NextOccurrenceInfo.builder()
                .occurrenceId(nextOccurrence.getId())
                .scheduledDate(nextOccurrence.getOccurrenceDate())
                .scheduledTime(nextOccurrence.getOccurrenceTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .dayOfWeek(Weekday.getDescriptionOfDayOfWeek(nextOccurrence.getOccurrenceDate().getDayOfWeek()))
                .scheduledAtUtc(AlarmScheduleCalculator.toInstant(
                    nextOccurrence.getOccurrenceDate(),
                    nextOccurrence.getOccurrenceTime(),
                    memberZone
                ).toString())
                .build())
            .build();
    }

    public static AlarmSyncResponse mapToSyncResponse(
        LocalDateTime serverTime,
        String timeZone,
        List<AlarmSyncItemDto> alarms
    ) {
        return AlarmSyncResponse.builder()
            .serverTime(serverTime)
            .timeZone(timeZone)
            .alarms(alarms)
            .build();
    }

    public static AlarmDeleteMethodResponse mapToAlarmDeleteMethodResponse(AlarmDeleteMethod deleteMethod) {
        return AlarmDeleteMethodResponse.builder()
            .deleteMethod(deleteMethod.name())
            .build();
    }

    private static String mapToStatusLabel(AlarmStatus status) {
        return status == AlarmStatus.INACTIVE ? "비활성화" : "활성화";
    }

    private static List<Weekday> mapToWeekdays(List<String> repeatDays) {
        if (repeatDays == null) return List.of();

        return repeatDays.stream()
            .map(Weekday::from)
            .filter(Objects::nonNull)
            .toList();
    }

    private static String truncateReason(String reason) {
        if (reason == null) return "";
        int maxLength = 2000;
        return reason.length() <= maxLength ? reason : reason.substring(0, maxLength);
    }
}
