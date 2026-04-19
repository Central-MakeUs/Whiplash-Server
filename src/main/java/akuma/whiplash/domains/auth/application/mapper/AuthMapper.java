package akuma.whiplash.domains.auth.application.mapper;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.application.dto.response.LoginResponse.MemberInfo;
import akuma.whiplash.domains.member.domain.contants.MemberStatus;
import akuma.whiplash.domains.member.domain.contants.Role;
import akuma.whiplash.domains.member.persistence.entity.MemberDeviceEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;

public class AuthMapper {

    private AuthMapper() {throw new IllegalArgumentException();}

    public static MemberEntity mapToMemberEntity(SocialMemberInfo memberInfo) {
        return MemberEntity.builder()
            .provider(memberInfo.provider())
            .providerUserId(memberInfo.providerUserId())
            .email(memberInfo.email())
            .nickname(memberInfo.name())
            .role(Role.USER)
            .status(MemberStatus.ACTIVE)
            .build();
    }

    public static MemberContext mapToMemberContext(MemberEntity memberEntity, String deviceId) {
        return MemberContext.builder()
            .role(memberEntity.getRole())
            .memberId(memberEntity.getId())
            .provider(memberEntity.getProvider())
            .email(memberEntity.getEmail())
            .nickname(memberEntity.getNickname())
            .deviceId(deviceId)
            .build();
    }

    public static MemberInfo mapToMemberInfo(MemberEntity member, boolean isNewMember) {
        return MemberInfo.builder()
            .memberId(member.getId())
            .provider(member.getProvider().name())
            .nickname(member.getNickname())
            .email(member.getEmail())
            .isNewMember(isNewMember)
            .status(member.getStatus().name())
            .build();
    }

    public static MemberDeviceEntity mapToMemberDeviceEntity(MemberEntity member, SocialLoginRequest request) {
        return MemberDeviceEntity.builder()
            .member(member)
            .deviceId(request.deviceId())
            .platform(request.platform())
            .fcmToken(request.fcmToken())
            .isLoggedIn(true)
            .appVersion(request.appVersion())
            .osVersion(request.osVersion())
            .build();
    }
}
