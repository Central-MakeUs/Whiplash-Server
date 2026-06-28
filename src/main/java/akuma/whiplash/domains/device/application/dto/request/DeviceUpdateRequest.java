package akuma.whiplash.domains.device.application.dto.request;

import akuma.whiplash.global.validation.annotation.IanaTimeZoneFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "기기 정보 갱신 요청 DTO")
public record DeviceUpdateRequest(

    @Schema(description = "디바이스 UUID", example = "device-uuid")
    @NotBlank(message = "디바이스 ID를 입력해주세요.")
    String deviceId,

    @Schema(description = "플랫폼", example = "ANDROID")
    @NotBlank(message = "플랫폼을 입력해주세요.")
    String platform,

    @Schema(description = "FCM 푸시 토큰", example = "fcm-token")
    @NotBlank(message = "FCM 토큰을 입력해주세요.")
    String fcmToken,

    @Schema(description = "앱 버전", example = "1.0.0")
    String appVersion,

    @Schema(description = "OS 버전", example = "14")
    String osVersion,

    @Schema(description = "IANA time zone", example = "Asia/Seoul")
    @NotBlank(message = "timeZone을 입력해주세요.")
    @IanaTimeZoneFormat
    String timeZone
) {}
