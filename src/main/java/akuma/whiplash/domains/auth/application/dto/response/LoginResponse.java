package akuma.whiplash.domains.auth.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "소셜 로그인 응답 DTO")
@Builder
public record LoginResponse(
    String accessToken,
    String refreshToken,
    String nickname
) {
}