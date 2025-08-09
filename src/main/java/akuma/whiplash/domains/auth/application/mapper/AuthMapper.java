package akuma.whiplash.domains.auth.application.mapper;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.member.domain.contants.Role;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import java.time.LocalDateTime;

public class AuthMapper {

    private AuthMapper() {throw new IllegalArgumentException();}

    public static MemberEntity mapToMemberEntity(SocialMemberInfo memberInfo) {
        return MemberEntity.builder()
            .socialId(memberInfo.socialId())
            .email(memberInfo.email())
            .nickname(memberInfo.name())
            .role(Role.USER)
            .privacyPolicy(true)
            .pushNotificationPolicy(true)
            .privacyAgreedAt(LocalDateTime.now())
            .pushAgreedAt(LocalDateTime.now())
            .build();
    }

    public static MemberContext mapToMemberContext(MemberEntity memberEntity, String deviceId) {
        return MemberContext.builder()
            .role(memberEntity.getRole())
            .memberId(memberEntity.getId())
            .socialId(memberEntity.getSocialId())
            .email(memberEntity.getEmail())
            .nickname(memberEntity.getNickname())
            .deviceId(deviceId)
            .build();
    }
}
