package akuma.whiplash.domains.member.application.usecase;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.member.application.dto.request.MemberPrivacyPolicyModifyRequest;
import akuma.whiplash.domains.member.application.dto.request.MemberPushNotificationPolicyModifyRequest;
import akuma.whiplash.domains.member.domain.service.MemberCommandService;
import akuma.whiplash.global.annotation.architecture.UseCase;
import lombok.RequiredArgsConstructor;


@UseCase
@RequiredArgsConstructor
public class MemberUseCase {

    private final MemberCommandService memberCommandService;

    public void modifyMemberPrivacyPolicy(Long memberId, MemberPrivacyPolicyModifyRequest request) {
        memberCommandService.modifyPrivacyPolicy(memberId, request.privacyPolicy());
    }

    public void modifyMemberPushNotificationPolicy(Long memberId, MemberPushNotificationPolicyModifyRequest request) {
        memberCommandService.modifyPushNotificationPolicy(memberId, request.pushNotificationPolicy());
    }

    public void hardDeleteMember(MemberContext memberContext) {
        memberCommandService.hardDeleteMember(memberContext.memberId(), memberContext.deviceId());
    }
}
