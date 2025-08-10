package akuma.whiplash.domains.member.presentation;

import static akuma.whiplash.domains.member.exception.MemberErrorCode.*;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.member.application.dto.request.MemberPrivacyPolicyModifyRequest;
import akuma.whiplash.domains.member.application.dto.request.MemberPushNotificationPolicyModifyRequest;
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
    @Operation(summary = "회원 개인정보 수집 동의 변경", description = "개인정보 수집 동의 여부를 변경합니다.")
    @PutMapping("/terms/privacy")
    public ApplicationResponse<Void> modifyPrivacyPolicy(@AuthenticationPrincipal MemberContext memberContext, @RequestBody @Valid MemberPrivacyPolicyModifyRequest request) {
        memberUseCase.modifyMemberPrivacyPolicy(memberContext.memberId(), request);
        return ApplicationResponse.onSuccess();
    }

    @CustomErrorCodes(memberErrorCodes = {MEMBER_NOT_FOUND})
    @Operation(summary = "회원 푸시 알림 수신 동의 변경", description = "푸시 알림 수신 동의 여부를 변경합니다.")
    @PutMapping("/terms/push-notifications")
    public ApplicationResponse<Void> modifyPushNotificationPolicy(@AuthenticationPrincipal MemberContext memberContext, @RequestBody @Valid MemberPushNotificationPolicyModifyRequest request) {
        memberUseCase.modifyMemberPushNotificationPolicy(memberContext.memberId(), request);
        return ApplicationResponse.onSuccess();
    }

    @CustomErrorCodes(memberErrorCodes = {MEMBER_NOT_FOUND})
    @Operation(summary = "회원 탈퇴", description = "회원 정보, 관련된 알람 정보를 hard delete 합니다.")
    @DeleteMapping
    public ApplicationResponse<Void> hardDeleteMember(@AuthenticationPrincipal MemberContext memberContext) {
        memberUseCase.hardDeleteMember(memberContext.memberId());
        return ApplicationResponse.onSuccess();
    }
}
