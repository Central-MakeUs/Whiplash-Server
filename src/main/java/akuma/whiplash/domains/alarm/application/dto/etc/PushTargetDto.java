package akuma.whiplash.domains.alarm.application.dto.etc;

import lombok.Builder;

@Builder
public record PushTargetDto(
    String token,
    String address,
    Long memberId,
    Long occurrenceId
) {

}
