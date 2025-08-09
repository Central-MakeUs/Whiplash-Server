package akuma.whiplash.domains.alarm.application.dto.etc;

import lombok.Builder;

@Builder
public record OccurrencePushInfo(
    Long occurrenceId,
    Long memberId,
    String address
) {

}
