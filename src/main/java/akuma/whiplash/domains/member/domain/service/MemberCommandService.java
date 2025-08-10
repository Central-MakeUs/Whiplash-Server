package akuma.whiplash.domains.member.domain.service;


public interface MemberCommandService {
    void modifyPrivacyPolicy(Long memberId, Boolean privacyPolicy);
    void modifyPushNotificationPolicy(Long memberId, Boolean pushNotificationPolicy);
    void hardDeleteMember(Long memberId);
}
