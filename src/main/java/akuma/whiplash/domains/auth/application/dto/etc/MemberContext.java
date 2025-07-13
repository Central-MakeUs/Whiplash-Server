package akuma.whiplash.domains.auth.application.dto.etc;

import lombok.Builder;

@Builder
public record MemberContext(
    Long memberId,
    String socialId,
    String email,
    String nickname
) {

}
