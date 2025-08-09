package akuma.whiplash.domains.auth.application.usecase;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.application.dto.request.LogoutRequest;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.application.dto.response.LoginResponse;
import akuma.whiplash.domains.auth.application.dto.response.TokenResponse;
import akuma.whiplash.domains.auth.domain.service.AuthCommandService;
import akuma.whiplash.global.annotation.architecture.UseCase;
import akuma.whiplash.infrastructure.firebase.FcmService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AuthUseCase {

    private final AuthCommandService authCommandService;
    private final FcmService fcmService;

    public LoginResponse socialLogin(SocialLoginRequest request) {
        return authCommandService.login(request);
    }

    public void logout(LogoutRequest request, Long memberId) {
        authCommandService.logout(request, memberId);
    }

    public TokenResponse reissueToken(MemberContext memberContext) {
        return authCommandService.reissueToken(memberContext);
    }

    public void registerFcmToken(Long memberId, String deviceId, String fcmToken) {
        fcmService.registerFcmToken(memberId, deviceId, fcmToken);
    }
}
