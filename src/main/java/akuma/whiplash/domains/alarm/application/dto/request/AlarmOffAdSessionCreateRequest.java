package akuma.whiplash.domains.alarm.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "광고 시청으로 알람 끄기 세션 발급 요청 DTO")
public record AlarmOffAdSessionCreateRequest(
    @Schema(description = "광고로 끌 알람 발생 건 ID", example = "1")
    @NotNull(message = "알람 발생 건 ID를 입력해주세요.")
    Long occurrenceId
) {
}
