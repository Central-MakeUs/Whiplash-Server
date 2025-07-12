package akuma.whiplash.domains.auth.application.mapper;

import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.member.domain.contants.Role;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;

public class AuthMapper {

    private AuthMapper() {throw new IllegalArgumentException();}

    public static MemberEntity toMemberEntity(SocialMemberInfo memberInfo) {
        return MemberEntity.builder()
            .socialId(memberInfo.socialId())
            .email(memberInfo.email())
            .nickname(memberInfo.name())
            .role(Role.USER)
            .build();
    }
}
