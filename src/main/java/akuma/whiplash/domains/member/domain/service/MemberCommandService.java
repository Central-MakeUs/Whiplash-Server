package akuma.whiplash.domains.member.domain.service;

public interface MemberCommandService {
    void modifyPrivacyPolicy(Long memberId, boolean privacyPolicy);
    void modifyPushNotificationPolicy(Long memberId, boolean pushNotificationPolicy);
    void hardDeleteMember(Long memberId, String deviceId);
}
