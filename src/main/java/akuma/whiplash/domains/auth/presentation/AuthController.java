package akuma.whiplash.domains.auth.presentation;

import static akuma.whiplash.domains.auth.exception.AuthErrorCode.*;
import static akuma.whiplash.global.response.code.CommonErrorCode.*;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.application.dto.request.RegisterFcmTokenRequest;
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
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    @CustomErrorCodes(
        commonErrorCodes = {BAD_REQUEST},
        authErrorCodes = {UNSUPPORTED_SOCIAL_TYPE, IS_SLEEPER_ACCOUNT}
    )
    @Operation(summary = "소셜 로그인", description = "구글, 애플, 카카오 소셜 로그인을 지원합니다.")
    @PostMapping("/social-login")
    public ApplicationResponse<LoginResponse> login(@RequestBody @Valid SocialLoginRequest request) {
        LoginResponse response = authUseCase.socialLogin(request);
        return ApplicationResponse.onSuccess(response);
    }

    @CustomErrorCodes(commonErrorCodes = {BAD_REQUEST})
    @Operation(summary = "로그아웃", description = "리프레시 토큰을 삭제합니다. Authorization 헤더에는 리프레시 토큰을 담아서 요청합니다.")
    @PostMapping("/logout")
    public ApplicationResponse<Void> logout(@AuthenticationPrincipal MemberContext memberContext) {
        authUseCase.logout(memberContext);
        return ApplicationResponse.onSuccess();
    }

    @CustomErrorCodes(
        commonErrorCodes = {BAD_REQUEST},
        authErrorCodes = {INVALID_TOKEN}
    )
    @Operation(summary = "토큰 재발급", description = "액세스, 리프레시 토큰을 재발급합니다. Authorization 헤더에는 리프레시 토큰을 담아서 요청합니다.")
    @PostMapping("/reissue")
    public ApplicationResponse<TokenResponse> reissueToken(@AuthenticationPrincipal MemberContext memberContext) {

        TokenResponse tokenResponse = authUseCase.reissueToken(memberContext);
        return ApplicationResponse.onSuccess(tokenResponse);
    }

    @CustomErrorCodes(commonErrorCodes = {BAD_REQUEST})
    @Operation(summary = "FCM 토큰 등록", description = "FCM 토큰을 등록합니다.")
    @PostMapping("/fcm-token")
    public ApplicationResponse<Void> registerFcmToken(
        @AuthenticationPrincipal MemberContext memberContext,
        @Valid @RequestBody RegisterFcmTokenRequest request) {
        authUseCase.registerFcmToken(memberContext.memberId(), memberContext.deviceId(), request.fcmToken());
        return ApplicationResponse.onSuccess();
    }
}
