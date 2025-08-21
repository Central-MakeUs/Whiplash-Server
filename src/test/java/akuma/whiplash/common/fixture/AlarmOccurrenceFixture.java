package akuma.whiplash.common.fixture;

import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;

@Getter
public enum AlarmOccurrenceFixture {

    ALARM_OCCURRENCE_01(
        LocalDate.now(),
        LocalTime.now().minusMinutes(10),
        DeactivateType.NONE
    ),
    ALARM_OCCURRENCE_02(
        LocalDate.now(),
        LocalTime.now().plusMinutes(10),
        DeactivateType.NONE
    ),
    ALARM_OCCURRENCE_PAST_TIME(
        LocalDate.now(),
        LocalTime.of(0, 0),
        DeactivateType.NONE
    ),
    ALARM_OCCURRENCE_FUTURE_TIME(
        LocalDate.now(),
        LocalTime.of(23, 59),
        DeactivateType.NONE
    ),
    ALARM_OCCURRENCE_DEACTIVATED(
        LocalDate.now(),
        LocalTime.now().minusMinutes(10),
        DeactivateType.OFF
    );

    private final LocalDate date;
    private final LocalTime time;
    private final DeactivateType deactivateType;

    AlarmOccurrenceFixture(LocalDate date, LocalTime time, DeactivateType deactivateType) {
        this.date = date;
        this.time = time;
        this.deactivateType = deactivateType;
    }

    public AlarmOccurrenceEntity toEntity(AlarmEntity alarm) {
        return AlarmOccurrenceEntity.builder()
            .alarm(alarm)
            .date(date)
            .time(time)
            .deactivateType(deactivateType)
            .alarmRinging(false)
            .ringingCount(0)
            .reminderSent(false)
            .build();
    }

    public AlarmOccurrenceEntity toEntity(AlarmEntity alarm, LocalDate date, LocalTime time, DeactivateType deactivateType) {
        return AlarmOccurrenceEntity.builder()
            .alarm(alarm)
            .date(date)
            .time(time)
            .deactivateType(deactivateType)
            .alarmRinging(false)
            .ringingCount(0)
            .reminderSent(false)
            .build();
    }
}
