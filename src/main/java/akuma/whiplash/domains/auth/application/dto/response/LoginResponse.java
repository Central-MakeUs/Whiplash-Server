package akuma.whiplash.domains.auth.application.dto.response;

import lombok.Builder;

@Builder
public record LoginResponse(
    String accessToken,
    String refreshToken,
    String nickname
) {
}