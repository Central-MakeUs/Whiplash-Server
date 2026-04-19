package akuma.whiplash.domains.auth.application.dto.etc;

import akuma.whiplash.domains.member.domain.contants.Role;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import lombok.Builder;

@Builder
public record MemberContext(
    Role role,
    Long memberId,
    SocialType provider,
    String email,
    String nickname,
    String deviceId
) {}
