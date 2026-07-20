package akuma.whiplash.domains.alarm.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record AlarmSyncResponse(
    LocalDateTime serverTime,
    String timeZone,
    List<AlarmSyncItemDto> alarms
) {
}
