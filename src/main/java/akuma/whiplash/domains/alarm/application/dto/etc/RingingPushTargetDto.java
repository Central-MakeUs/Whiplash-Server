package akuma.whiplash.domains.alarm.application.dto.etc;

import lombok.Builder;

@Builder
public record RingingPushTargetDto(
    String token,
    Long alarmId,
    Long memberId
) {
}