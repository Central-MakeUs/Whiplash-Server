package akuma.whiplash.domains.alarm.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
@Schema(description = "다음 알람 발생 정보 응답 DTO")
public record NextOccurrenceResponse(
    @Schema(description = "알람 발생 PK", example = "1")
    Long occurrenceId,

    @Schema(description = "예정 시각 (ISO 8601)", example = "2024-04-19T08:30:00")
    LocalDateTime scheduledAt,

    @Schema(description = "요일 (한글)", example = "월요일")
    String dayOfWeek
) {
}
