package akuma.whiplash.domains.member.presentation;

import static akuma.whiplash.domains.member.exception.MemberErrorCode.*;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.member.application.dto.request.MemberTermsModifyRequest;
import akuma.whiplash.domains.member.application.usecase.MemberUseCase;
import akuma.whiplash.global.annotation.swagger.CustomErrorCodes;
import akuma.whiplash.global.response.ApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberUseCase memberUseCase;

    @CustomErrorCodes(memberErrorCodes = {MEMBER_NOT_FOUND})
    @Operation(summary = "회원 약관 동의 정보 변경", description = "개인정보 제공 동의, 푸시 알림 수신 동의 여부를 변경합니다.")
    @PutMapping("/terms")
    public ApplicationResponse<Void> modifyMemberTermsInfo(@AuthenticationPrincipal MemberContext memberContext, @RequestBody @Valid MemberTermsModifyRequest request) {
        memberUseCase.modifyMemberTermsInfo(memberContext.memberId(), request);
        return ApplicationResponse.onSuccess();
    }

    @CustomErrorCodes
    @Operation(summary = "회원 탈퇴", description = "회원 정보, 관련된 알람 정보를 hard delete 합니다.")
    @DeleteMapping
    public ApplicationResponse<Void> hardDeleteMember(@AuthenticationPrincipal MemberContext memberContext) {
        memberUseCase.hardDeleteMember(memberContext.memberId());
        return ApplicationResponse.onSuccess();
    }
}
