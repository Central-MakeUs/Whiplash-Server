package akuma.whiplash.domains.alarm.application.dto.response;

import lombok.Builder;

@Builder
public record CreateAlarmOccurrenceResponse(
    Long occurrenceId
) {

}
