package akuma.whiplash.domains.member.domain.service;

public interface MemberCommandService {
    void deleteMember(Long memberId, String deviceId);
}
