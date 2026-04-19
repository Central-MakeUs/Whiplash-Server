package akuma.whiplash.domains.auth.application.dto.etc;

import akuma.whiplash.domains.member.domain.contants.SocialType;
import lombok.Builder;

@Builder
public record SocialMemberInfo(
    SocialType provider,
    String providerUserId,
    String email,
    String name
) {}
