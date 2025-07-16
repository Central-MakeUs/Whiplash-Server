package akuma.whiplash.domains.auth.application.usecase;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.application.dto.request.LogoutRequest;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.application.dto.response.LoginResponse;
import akuma.whiplash.domains.auth.application.dto.response.TokenResponse;
import akuma.whiplash.domains.auth.domain.service.AuthCommandService;
import akuma.whiplash.global.annotation.architecture.UseCase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AuthUseCase {

    private final AuthCommandService authCommandService;

    public LoginResponse socialLogin(SocialLoginRequest request) {
        return authCommandService.login(request);
    }

    public void logout(LogoutRequest request, Long memberId) {
        authCommandService.logout(request, memberId);
    }

    public TokenResponse reissueToken(String refreshToken, MemberContext memberContext, String deviceId) {
        return authCommandService.reissueToken(refreshToken, memberContext, deviceId);
    }
}
