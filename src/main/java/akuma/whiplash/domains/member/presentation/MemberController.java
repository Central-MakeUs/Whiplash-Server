package akuma.whiplash.domains.member.presentation;

import static akuma.whiplash.domains.member.exception.MemberErrorCode.MEMBER_NOT_FOUND;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.member.application.usecase.MemberUseCase;
import akuma.whiplash.global.annotation.swagger.CustomErrorCodes;
import akuma.whiplash.global.response.ApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberUseCase memberUseCase;

    @CustomErrorCodes(memberErrorCodes = {MEMBER_NOT_FOUND})
    @Operation(summary = "회원 탈퇴", description = "회원 정보, 관련된 알람 정보를 soft delete 합니다.")
    @DeleteMapping
    public ApplicationResponse<Void> softDeleteMember(@AuthenticationPrincipal MemberContext memberContext) {
        memberUseCase.softDeleteMember(memberContext);
        return ApplicationResponse.onSuccess();
    }
}
