package akuma.whiplash.domains.member.application.usecase;

import akuma.whiplash.domains.member.application.dto.request.MemberTermsModifyRequest;
import akuma.whiplash.domains.member.domain.service.MemberCommandService;
import akuma.whiplash.global.annotation.architecture.UseCase;
import lombok.RequiredArgsConstructor;


@UseCase
@RequiredArgsConstructor
public class MemberUseCase {

    private final MemberCommandService memberCommandService;

    public void modifyMemberTermsInfo(Long memberId, MemberTermsModifyRequest request) {
        memberCommandService.modifyMemberTermsInfo(memberId, request);
    }

    public void softDeleteMember(Long memberId) {
        memberCommandService.softDeleteMember(memberId);
    }
}
