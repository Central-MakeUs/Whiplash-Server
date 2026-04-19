package akuma.whiplash.domains.device.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "FCM 토큰 갱신 요청 DTO")
public record FcmTokenUpdateRequest(

    @Schema(description = "디바이스 UUID", example = "device-uuid")
    @NotBlank(message = "디바이스 ID를 입력해주세요.")
    String deviceId,

    @Schema(description = "새 FCM 토큰", example = "new-fcm-token")
    @NotBlank(message = "FCM 토큰을 입력해주세요.")
    String fcmToken
) {}
