package akuma.whiplash.domains.alarm.application.dto.etc;

import lombok.Builder;

@Builder
public record PushTargetDto(
    String token,
    Long memberId,
    Long alarmId,
    Long occurrenceId
) {

}
