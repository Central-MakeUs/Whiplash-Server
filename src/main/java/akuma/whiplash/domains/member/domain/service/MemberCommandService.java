package akuma.whiplash.domains.member.domain.service;

public interface MemberCommandService {
    void softDeleteMember(Long memberId, String deviceId);
}
