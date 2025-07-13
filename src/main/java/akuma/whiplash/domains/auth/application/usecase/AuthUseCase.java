package akuma.whiplash.domains.auth.application.usecase;

import akuma.whiplash.domains.auth.application.dto.request.LogoutRequest;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.application.dto.response.LoginResponse;
import akuma.whiplash.domains.auth.domain.service.AuthCommandService;
import akuma.whiplash.global.annotation.architecture.UseCase;
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
}
