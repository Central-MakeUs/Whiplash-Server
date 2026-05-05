package akuma.whiplash.domains.alarm.application.dto.response;

import lombok.Builder;

@Builder
public record AlarmDeleteByAdResponse(
    Long alarmId,
    Integer alarmRevision
) {
}
