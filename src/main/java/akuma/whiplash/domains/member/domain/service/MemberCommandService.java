package akuma.whiplash.domains.member.domain.service;

import akuma.whiplash.domains.member.application.dto.request.MemberTermsModifyRequest;

public interface MemberCommandService {
    void modifyMemberTermsInfo(Long memberId, MemberTermsModifyRequest request);
}
