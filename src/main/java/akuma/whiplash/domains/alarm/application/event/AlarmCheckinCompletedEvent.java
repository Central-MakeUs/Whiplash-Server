package akuma.whiplash.domains.alarm.application.event;

import java.time.LocalDateTime;

public record AlarmCheckinCompletedEvent(
    Long occurrenceId,
    Long memberId,
    String deviceId,
    Double latitude,
    Double longitude,
    LocalDateTime requestedAt,
    LocalDateTime processedAt
) {
}
