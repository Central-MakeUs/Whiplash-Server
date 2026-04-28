package akuma.whiplash.domains.alarm.application.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AlarmSyncItemDto(
    Long alarmId,
    int alarmRevision,
    String status,
    NextOccurrenceInfo nextOccurrence
) {

    @Builder
    public record NextOccurrenceInfo(
        Long occurrenceId,
        LocalDateTime scheduledAt
    ) {
    }
}
