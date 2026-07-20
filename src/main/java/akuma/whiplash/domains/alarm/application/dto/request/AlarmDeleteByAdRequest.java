package akuma.whiplash.domains.alarm.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "광고 시청으로 알람 삭제 요청 DTO")
public record AlarmDeleteByAdRequest(

    @Schema(description = "요청 디바이스 UUID", example = "device-uuid")
    @NotBlank(message = "디바이스 ID를 입력해주세요.")
    String deviceId,

    @Schema(description = "광고 시청 증빙 토큰", example = "ad-proof-token")
    @NotBlank(message = "광고 증빙 토큰을 입력해주세요.")
    String adProofToken
) {
}
