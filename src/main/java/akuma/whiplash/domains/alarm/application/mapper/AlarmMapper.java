package akuma.whiplash.domains.alarm.application.mapper;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmCheckinResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmDeleteByAdResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmDeleteByPaymentResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmPaymentResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmSyncItemDto;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmSyncResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import akuma.whiplash.domains.alarm.application.dto.response.NextOccurrenceResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmPreviewDto;
import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.domain.constant.DeactivationResult;
import akuma.whiplash.domains.alarm.domain.constant.AlarmStatus;
import akuma.whiplash.domains.alarm.domain.constant.DeleteType;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.domain.constant.SoundType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.exception.AlarmErrorCode;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmDeleteLogEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmDeactivationLogEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmRingingLogEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.util.date.DateUtil;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
            .revision(1)
            .build();
    }

    public static AlarmOccurrenceEntity mapToFirstAlarmOccurrenceEntity(AlarmEntity alarm, LocalDate nextDate, LocalTime alarmTime) {
        return AlarmOccurrenceEntity.builder()
            .alarm(alarm)
            .occurrenceDate(nextDate)
            .occurrenceTime(alarmTime)
            .scheduledAt(LocalDateTime.of(nextDate, alarmTime))
            .status(OccurrenceStatus.SCHEDULED)
            .alarmRinging(false)
            .ringingCount(0)
            .reminderSent(false)
            .build();
    }

    public static CreateAlarmResponse mapToCreateAlarmResponse(AlarmEntity alarm, AlarmOccurrenceEntity occurrence) {
        return CreateAlarmResponse.builder()
            .alarmId(alarm.getId())
            .alarmRevision(alarm.getRevision())
            .nextOccurrence(NextOccurrenceResponse.builder()
                .occurrenceId(occurrence.getId())
                .scheduledAt(occurrence.getScheduledAt())
                .dayOfWeek(DateUtil.getKoreanDayOfWeek(occurrence.getOccurrenceDate()))
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
        return AlarmOccurrenceEntity.builder()
            .alarm(alarm)
            .occurrenceDate(date)
            .occurrenceTime(alarm.getTime())
            .scheduledAt(LocalDateTime.of(date, alarm.getTime()))
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
        Double latitude,
        Double longitude,
        LocalDateTime requestedAt,
        LocalDateTime processedAt
    ) {
        return AlarmDeactivationLogEntity.builder()
            .alarmOccurrence(occurrence)
            .member(member)
            .paymentId(null)
            .deactivateType(DeactivateType.CHECKIN)
            .requestLatitude(latitude)
            .requestLongitude(longitude)
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
            .alarmRevision(alarm.getRevision())
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
            .alarmRevision(alarm.getRevision())
            .nextOccurrence(nextOccurrence == null ? null : AlarmPaymentResponse.NextOccurrenceInfo.builder()
                .occurrenceId(nextOccurrence.getId())
                .scheduledAt(nextOccurrence.getScheduledAt())
                .build())
            .build();
    }

    public static AlarmDeleteByPaymentResponse mapToAlarmDeleteByPaymentResponse(
        AlarmEntity alarm,
        LocalDateTime deletedAt
    ) {
        return AlarmDeleteByPaymentResponse.builder()
            .alarmId(alarm.getId())
            .deletedAt(deletedAt)
            .build();
    }

    public static AlarmDeleteByAdResponse mapToAlarmDeleteByAdResponse(AlarmEntity alarm) {
        return AlarmDeleteByAdResponse.builder()
            .alarmId(alarm.getId())
            .alarmRevision(alarm.getRevision())
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
            .requestLatitude(null)
            .requestLongitude(null)
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

    public static AlarmDeleteLogEntity mapToAdDeleteLogEntity(
        AlarmEntity alarm,
        MemberEntity member,
        String adProofToken,
        LocalDateTime requestedAt,
        LocalDateTime deletedAt
    ) {
        return AlarmDeleteLogEntity.builder()
            .alarm(alarm)
            .member(member)
            .deleteType(DeleteType.AD)
            .reason("")
            .adProofToken(adProofToken)
            .requestedAt(requestedAt)
            .deletedAt(deletedAt)
            .build();
    }

    public static AlarmPreviewDto mapToAlarmPreviewDto(
        AlarmEntity alarm,
        LocalDateTime now,
        AlarmOccurrenceEntity latestProcessedOccurrence,
        LocalDate firstDate,
        LocalDate secondDate,
        LocalDate thirdDate,
        Map<LocalDate, Long> occurrenceIdsByDate
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
            .nextOccurrence(AlarmPreviewDto.OccurrenceInfo.builder()
                .occurrenceId(occurrenceIdsByDate.get(resolvedNext))
                .scheduledDate(resolvedNext)
                .dayOfWeek(Weekday.getDescriptionOfDayOfWeek(resolvedNext.getDayOfWeek()))
                .build())
            .nextNextOccurrence(AlarmPreviewDto.OccurrenceInfo.builder()
                .occurrenceId(occurrenceIdsByDate.get(resolvedNextNext))
                .scheduledDate(resolvedNextNext)
                .dayOfWeek(Weekday.getDescriptionOfDayOfWeek(resolvedNextNext.getDayOfWeek()))
                .build())
            .build();
    }

    public static AlarmSyncItemDto mapToSyncItem(AlarmEntity alarm, AlarmOccurrenceEntity nextOccurrence) {
        return AlarmSyncItemDto.builder()
            .alarmId(alarm.getId())
            .alarmRevision(alarm.getRevision())
            .status(mapToStatusLabel(alarm.getStatus()))
            .nextOccurrence(nextOccurrence == null ? null : AlarmSyncItemDto.NextOccurrenceInfo.builder()
                .occurrenceId(nextOccurrence.getId())
                .scheduledAt(nextOccurrence.getScheduledAt())
                .build())
            .build();
    }

    public static AlarmSyncResponse mapToSyncResponse(LocalDateTime serverTime, List<AlarmSyncItemDto> alarms) {
        return AlarmSyncResponse.builder()
            .serverTime(serverTime)
            .alarms(alarms)
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
}
