package akuma.whiplash.domains.alarm.application.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AlarmPaymentResponse(
    Long alarmId,
    LocalDateTime deactivatedAt,
    NextOccurrenceInfo nextOccurrence
) {
    @Builder
    public record NextOccurrenceInfo(
        Long occurrenceId,
        LocalDateTime scheduledAt
    ) {
    }
}
