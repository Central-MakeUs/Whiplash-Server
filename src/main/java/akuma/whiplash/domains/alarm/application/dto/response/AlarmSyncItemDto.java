package akuma.whiplash.domains.alarm.application.dto.response;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record AlarmSyncItemDto(
    Long alarmId,
    String status,
    NextOccurrenceInfo nextOccurrence
) {

    @Builder
    public record NextOccurrenceInfo(
        Long occurrenceId,
        LocalDate scheduledDate,
        String scheduledTime,
        String dayOfWeek,
        String scheduledAtUtc
    ) {
    }
}
