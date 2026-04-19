package akuma.whiplash.domains.auth.application.dto.request;

import akuma.whiplash.domains.auth.presentation.util.annotation.SocialTypeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "소셜 로그인 요청 DTO")
public record SocialLoginRequest(

    @Schema(description = "소셜 프로바이더", example = "GOOGLE")
    @SocialTypeFormat(acceptedSocialTypes = {"GOOGLE", "APPLE", "KAKAO", "MOCK"})
    String provider,

    @Schema(description = "서드파티에서 발급받은 액세스 토큰", example = "provider-access-token")
    @NotBlank(message = "토큰을 입력해주세요")
    String providerAccessToken,

    @Schema(description = "클라이언트 디바이스 UUID", example = "device-uuid")
    @NotBlank(message = "디바이스 ID를 입력해주세요")
    String deviceId,

    @Schema(description = "플랫폼", example = "ANDROID")
    @NotBlank(message = "플랫폼을 입력해주세요")
    String platform,

    @Schema(description = "FCM 푸시 토큰", example = "fcm-token")
    String fcmToken,

    @Schema(description = "앱 버전", example = "1.0.0")
    String appVersion,

    @Schema(description = "OS 버전", example = "14")
    String osVersion
) {}
