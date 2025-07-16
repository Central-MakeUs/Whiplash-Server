package akuma.whiplash.domains.auth.presentation;

import static akuma.whiplash.global.response.code.CommonErrorCode.*;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.application.dto.request.LogoutRequest;
import akuma.whiplash.domains.auth.application.dto.request.ReissueRequest;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.application.dto.response.LoginResponse;
import akuma.whiplash.domains.auth.application.dto.response.TokenResponse;
import akuma.whiplash.domains.auth.application.usecase.AuthUseCase;
import akuma.whiplash.global.annotation.swagger.CustomErrorCodes;
import akuma.whiplash.global.response.ApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    @CustomErrorCodes(commonErrorCodes = {BAD_REQUEST})
    @Operation(summary = "소셜 로그인", description = "구글, 애플, 카카오 소셜 로그인을 지원합니다.")
    @PostMapping("/social-login")
    public ApplicationResponse<LoginResponse> login(@RequestBody @Valid SocialLoginRequest request) {
        LoginResponse response = authUseCase.socialLogin(request);
        return ApplicationResponse.onSuccess(response);
    }

    @CustomErrorCodes(commonErrorCodes = {BAD_REQUEST})
    @Operation(summary = "로그아웃", description = "리프레시 토큰을 삭제합니다.")
    @PostMapping("/logout")
    public ApplicationResponse<Void> logout(@AuthenticationPrincipal MemberContext memberContext, @RequestBody @Valid LogoutRequest request) {
        authUseCase.logout(request, memberContext.memberId());
        return ApplicationResponse.onSuccess();
    }

    @CustomErrorCodes(commonErrorCodes = {BAD_REQUEST})
    @Operation(summary = "토큰 재발급", description = "액세스, 리프레시 토큰을 재발급합니다.")
    @PostMapping("/reissue")
    public ApplicationResponse<TokenResponse> reissueToken(HttpServletRequest request, @AuthenticationPrincipal MemberContext memberContext, @RequestBody @Valid ReissueRequest reissueRequest) {
        String refreshToken = request.getHeader("Authorization").split(" ")[1];
        TokenResponse tokenResponse = authUseCase.reissueToken(refreshToken, memberContext, reissueRequest.deviceId());
        return ApplicationResponse.onSuccess(tokenResponse);
    }
}
