package akuma.whiplash.domains.alarm.application.dto.etc;

import lombok.Builder;

@Builder
public record RingingPushInfo(
    Long occurrenceId,
    Long alarmId,
    Long memberId
) {
}
