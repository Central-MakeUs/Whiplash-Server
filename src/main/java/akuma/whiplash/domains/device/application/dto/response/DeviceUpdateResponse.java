package akuma.whiplash.domains.device.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

@Schema(description = "기기 정보 갱신 응답 DTO")
@Builder
public record DeviceUpdateResponse(

    @Schema(description = "디바이스 UUID")
    String deviceId,

    @Schema(description = "플랫폼")
    String platform,

    @Schema(description = "갱신된 FCM 토큰")
    String fcmToken,

    @Schema(description = "앱 버전")
    String appVersion,

    @Schema(description = "OS 버전")
    String osVersion,

    @Schema(description = "IANA time zone")
    String timeZone,

    @Schema(description = "갱신 시각 (ISO_LOCAL_DATE_TIME)")
    LocalDateTime updatedAt
) {}
