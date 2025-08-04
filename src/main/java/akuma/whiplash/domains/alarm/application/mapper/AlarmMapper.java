package akuma.whiplash.domains.alarm.application.mapper;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmInfoPreviewResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.domain.constant.SoundType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.exception.AlarmErrorCode;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOffLogEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.global.exception.ApplicationException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class AlarmMapper {

    public static AlarmEntity mapToAlarmEntity(AlarmRegisterRequest request, MemberEntity memberEntity) {
        return AlarmEntity.builder()
            .member(memberEntity)
            .alarmPurpose(request.alarmPurpose())
            .time(request.time())
            .repeatDays(mapToWeekdays(request.repeatDays()))
            .soundType(SoundType.from(request.soundType()))
            .latitude(request.latitude())
            .longitude(request.longitude())
            .address(request.address())
            .memberActiveStatus(true)
            .build();
    }

    public static AlarmOffLogEntity mapToAlarmOffLogEntity(AlarmEntity alarm, MemberEntity member) {
        return AlarmOffLogEntity.builder()
            .alarm(alarm)
            .member(member)
            .build();
    }

    public static AlarmOccurrenceEntity mapToTodayFirstAlarmOccurrenceEntity(AlarmEntity alarmEntity) {
        LocalDate today = LocalDate.now();
        DayOfWeek todayDayOfWeek = today.getDayOfWeek();

        boolean isTodayAlarmDay = alarmEntity.getRepeatDays().stream()
            .anyMatch(weekday -> weekday.getDayOfWeek() == todayDayOfWeek);

        if (!isTodayAlarmDay) {
            throw ApplicationException.from(AlarmErrorCode.TODAY_IS_NOT_ALARM_DAY); // 오늘은 울릴 날이 아님
        }

        return AlarmOccurrenceEntity.builder()
            .alarm(alarmEntity)
            .date(today)
            .time(alarmEntity.getTime())
            .deactivateType(DeactivateType.NONE)
            .checkinTime(null)
            .alarmRinging(true)
            .deactivatedAt(null)
            .ringingCount(1)
            .memberActiveStatus(true)
            .build();
    }

    public static AlarmOccurrenceEntity mapToAlarmOccurrenceForDate(AlarmEntity alarm, LocalDate date) {
        return AlarmOccurrenceEntity.builder()
            .alarm(alarm)
            .date(date)
            .time(alarm.getTime())
            .deactivateType(DeactivateType.NONE)
            .deactivatedAt(null)
            .checkinTime(null)
            .alarmRinging(false)
            .ringingCount(0)
            .memberActiveStatus(true)
            .build();
    }

    public static CreateAlarmOccurrenceResponse mapToCreateAlarmOccurrenceResponse(Long occurrenceId) {
        return CreateAlarmOccurrenceResponse.builder()
            .occurrenceId(occurrenceId)
            .build();
    }

    private static List<Weekday> mapToWeekdays(List<String> repeatDays) {
        if (repeatDays == null) return List.of();

        return repeatDays.stream()
            .map(Weekday::from)
            .filter(Objects::nonNull)
            .toList();
    }
}

