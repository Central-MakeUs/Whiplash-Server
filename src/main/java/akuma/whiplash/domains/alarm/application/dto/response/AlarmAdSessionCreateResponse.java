package akuma.whiplash.domains.alarm.application.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AlarmAdSessionCreateResponse(
    String adSessionId,
    LocalDateTime expiresAt
) {
}
