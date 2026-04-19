package akuma.whiplash.domains.device.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

@Schema(description = "FCM 토큰 갱신 응답 DTO")
@Builder
public record FcmTokenUpdateResponse(

    @Schema(description = "디바이스 UUID")
    String deviceId,

    @Schema(description = "갱신된 FCM 토큰")
    String fcmToken,

    @Schema(description = "갱신 시각 (ISO_LOCAL_DATE_TIME)")
    LocalDateTime updatedAt
) {}
