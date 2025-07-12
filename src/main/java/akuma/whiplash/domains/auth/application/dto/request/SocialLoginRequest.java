package akuma.whiplash.domains.auth.application.dto.request;


import akuma.whiplash.domains.auth.presentation.util.annotation.SocialTypeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(

    @Schema(description = "소셜 타입", example = "GOOGLE")
    @SocialTypeFormat(acceptedSocialTypes = {"GOOGLE", "APPLE", "KAKAO"})
    String socialType,

    @Schema(description = "토큰", example = "213jkdd")
    @NotBlank(message = "토큰을 입력해주세요")
    String token,

    @Schema(description = "디바이스 ID", example = "dsjk23121m3")
    @NotBlank(message = "디바이스 ID를 입력해주세요")
    String deviceId,

    @Schema(description = "nonce 암호화 하기 전 raw값(애플 로그인시에만 필요)")
    String originalNonce
) {}