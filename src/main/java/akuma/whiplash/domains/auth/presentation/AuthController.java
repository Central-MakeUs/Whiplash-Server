package akuma.whiplash.domains.auth.presentation;

import static akuma.whiplash.domains.auth.exception.AuthErrorCode.INVALID_TOKEN;
import static akuma.whiplash.domains.auth.exception.AuthErrorCode.TOKEN_EXPIRED;
import static akuma.whiplash.domains.auth.exception.AuthErrorCode.UNSUPPORTED_SOCIAL_TYPE;
import static akuma.whiplash.domains.member.exception.MemberErrorCode.MEMBER_DELETED;
import static akuma.whiplash.global.response.code.CommonErrorCode.BAD_REQUEST;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.application.dto.response.LoginResponse;
import akuma.whiplash.domains.auth.application.dto.response.TokenResponse;
import akuma.whiplash.domains.auth.application.usecase.AuthUseCase;
import akuma.whiplash.global.annotation.swagger.CustomErrorCodes;
import akuma.whiplash.global.response.ApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    @CustomErrorCodes(
        commonErrorCodes = {BAD_REQUEST},
        authErrorCodes = {UNSUPPORTED_SOCIAL_TYPE},
        memberErrorCodes = {MEMBER_DELETED}
    )
    @Operation(summary = "소셜 로그인", description = "구글, 애플, 카카오 소셜 로그인을 지원합니다.")
    @PostMapping("/social-login")
    public ApplicationResponse<LoginResponse> login(@RequestBody @Valid SocialLoginRequest request) {
        LoginResponse response = authUseCase.socialLogin(request);
        return ApplicationResponse.onSuccess(response);
    }

    @CustomErrorCodes(authErrorCodes = {INVALID_TOKEN, TOKEN_EXPIRED})
    @Operation(summary = "로그아웃", description = "현재 디바이스를 로그아웃합니다. Authorization 헤더에는 액세스 토큰을 담아서 요청합니다.")
    @PostMapping("/logout")
    public ApplicationResponse<Void> logout(@AuthenticationPrincipal MemberContext memberContext) {
        authUseCase.logout(memberContext);
        return ApplicationResponse.onSuccess();
    }

    @CustomErrorCodes(authErrorCodes = {INVALID_TOKEN, TOKEN_EXPIRED})
    @Operation(summary = "토큰 재발급", description = "액세스, 리프레시 토큰을 재발급합니다. Authorization 헤더에는 리프레시 토큰을 담아서 요청합니다.")
    @PostMapping("/token/reissue")
    public ApplicationResponse<TokenResponse> reissueToken(@AuthenticationPrincipal MemberContext memberContext) {
        TokenResponse tokenResponse = authUseCase.reissueToken(memberContext);
        return ApplicationResponse.onSuccess(tokenResponse);
    }
}
