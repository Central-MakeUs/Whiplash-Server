package akuma.whiplash.domains.auth.application.dto.etc;

import akuma.whiplash.domains.member.domain.contants.SocialType;
import lombok.Builder;

@Builder
public record SocialMemberInfo (
    String socialId,    // 플랫폼_ID(ex: KAKAO_2y8dnbk33dd)
    String email,
    String name
) {

}