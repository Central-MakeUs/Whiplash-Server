package akuma.whiplash.domains.alarm.application.dto.etc;

import lombok.Builder;

@Builder
public record AlarmOccurrenceCreateBatchResult(
    int createdCount,
    int skippedCount,
    int failedCount
) {
}