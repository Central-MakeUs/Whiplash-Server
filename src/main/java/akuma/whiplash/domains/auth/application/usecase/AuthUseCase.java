package akuma.whiplash.domains.auth.application.usecase;

import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.application.dto.response.LoginResponse;
import akuma.whiplash.domains.auth.domain.service.AuthCommandServiceImpl;
import akuma.whiplash.global.annotation.architecture.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AuthUseCase {

    private final AuthCommandServiceImpl authCommandServiceImpl;

    public LoginResponse socialLogin(SocialLoginRequest request) {
        return authCommandServiceImpl.login(request);
    }
}
