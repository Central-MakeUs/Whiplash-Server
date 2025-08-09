package akuma.whiplash.domains.auth.application.dto.etc;

import akuma.whiplash.domains.member.domain.contants.Role;
import lombok.Builder;

@Builder
public record MemberContext(
    Role role,
    Long memberId,
    String socialId,
    String email,
    String nickname,
    String deviceId
) {

}
