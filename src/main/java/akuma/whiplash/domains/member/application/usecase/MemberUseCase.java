package akuma.whiplash.domains.member.application.usecase;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.member.domain.service.MemberCommandService;
import akuma.whiplash.global.annotation.architecture.UseCase;
import lombok.RequiredArgsConstructor;


@UseCase
@RequiredArgsConstructor
public class MemberUseCase {

    private final MemberCommandService memberCommandService;

    public void deleteMember(MemberContext memberContext) {
        memberCommandService.deleteMember(memberContext.memberId(), memberContext.deviceId());
    }
}
