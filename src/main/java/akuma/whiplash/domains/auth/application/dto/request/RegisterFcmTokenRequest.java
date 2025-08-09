package akuma.whiplash.domains.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "FCM 토큰 등록 요청 DTO")
public record RegisterFcmTokenRequest(

    @Schema(description = "FCM 토큰", example = "djkhsa01whjas")
    @NotBlank
    String fcmToken
) {}