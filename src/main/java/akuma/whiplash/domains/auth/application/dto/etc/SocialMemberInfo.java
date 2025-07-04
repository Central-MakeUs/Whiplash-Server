package akuma.whiplash.domains.auth.application.dto.etc;

import akuma.whiplash.domains.member.domain.contants.SocialType;
import lombok.Builder;

@Builder
public record SocialMemberInfo (
    String socialId,    // Google UID
    String email,
    String name,
    SocialType socialType    // "GOOGLE", "APPLE", "KAKAO"
) {

}