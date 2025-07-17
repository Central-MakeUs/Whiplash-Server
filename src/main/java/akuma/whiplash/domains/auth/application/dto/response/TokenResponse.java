package akuma.whiplash.domains.auth.application.dto.response;

import lombok.Builder;

@Builder
public record TokenResponse(
    String accessToken,
    String refreshToken
) {

}
