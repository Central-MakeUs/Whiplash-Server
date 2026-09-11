package akuma.whiplash.domains.alarm.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "광고 검증 세션 실행 요청 DTO")
public record AlarmAdActionRequest(
    @Schema(description = "백엔드가 발급한 광고 검증 세션 ID", example = "ad-session-id")
    @NotBlank(message = "광고 세션 ID를 입력해주세요.")
    String adSessionId
) {
}
