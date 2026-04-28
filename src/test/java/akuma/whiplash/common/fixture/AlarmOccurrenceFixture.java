package akuma.whiplash.common.fixture;

import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
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
        OccurrenceStatus.SCHEDULED
    ),
    ALARM_OCCURRENCE_02(
        LocalDate.now(),
        LocalTime.now().plusMinutes(10),
        OccurrenceStatus.SCHEDULED
    ),
    ALARM_OCCURRENCE_PAST_TIME(
        LocalDate.now(),
        LocalTime.of(0, 0),
        OccurrenceStatus.SCHEDULED
    ),
    ALARM_OCCURRENCE_FUTURE_TIME(
        LocalDate.now(),
        LocalTime.of(23, 59),
        OccurrenceStatus.SCHEDULED
    ),
    ALARM_OCCURRENCE_DEACTIVATED(
        LocalDate.now(),
        LocalTime.now().minusMinutes(10),
        OccurrenceStatus.CHECKIN
    );

    private final LocalDate date;
    private final LocalTime time;
    private final OccurrenceStatus status;

    AlarmOccurrenceFixture(LocalDate date, LocalTime time, OccurrenceStatus status) {
        this.date = date;
        this.time = time;
        this.status = status;
    }

    public AlarmOccurrenceEntity toEntity(AlarmEntity alarm) {
        return AlarmOccurrenceEntity.builder()
            .alarm(alarm)
            .occurrenceDate(date)
            .occurrenceTime(time)
            .scheduledAt(java.time.LocalDateTime.of(date, time))
            .status(status)
            .alarmRinging(false)
            .ringingCount(0)
            .reminderSent(false)
            .build();
    }

    public AlarmOccurrenceEntity toEntity(AlarmEntity alarm, LocalDate date, LocalTime time, OccurrenceStatus status) {
        return AlarmOccurrenceEntity.builder()
            .alarm(alarm)
            .occurrenceDate(date)
            .occurrenceTime(time)
            .scheduledAt(java.time.LocalDateTime.of(date, time))
            .status(status)
            .alarmRinging(false)
            .ringingCount(0)
            .reminderSent(false)
            .build();
    }
}
