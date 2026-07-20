package akuma.whiplash.domains.alarm.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;

@Builder
@Schema(description = "다음 알람 발생 정보 응답 DTO")
public record NextOccurrenceResponse(
    @Schema(description = "알람 발생 PK", example = "1")
    Long occurrenceId,

    @Schema(description = "현재 요청 기기 timeZone 기준 예정 날짜", example = "2026-06-29")
    LocalDate scheduledDate,

    @Schema(description = "현재 요청 기기 timeZone 기준 예정 시간 (HH:mm)", example = "07:00")
    String scheduledTime,

    @Schema(description = "현재 요청 기기 timeZone 기준 요일 단축형", example = "월")
    String dayOfWeek,

    @Schema(description = "UTC 기준 실제 알림 실행 시각", example = "2026-06-28T22:00:00Z")
    String scheduledAtUtc
) {
}
