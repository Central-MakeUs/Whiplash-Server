package akuma.whiplash.domains.auth.application.usecase;

import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.application.dto.response.AuthResponse;
import akuma.whiplash.domains.auth.domain.service.SocialLoginService;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import akuma.whiplash.global.annotation.architecture.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AuthUseCase {

    private final SocialLoginService socialLoginService;

    public AuthResponse socialLogin(SocialLoginRequest request) {
        return socialLoginService.login(
            SocialType.valueOf(request.socialType()),
            request.token(),
            request.deviceId()
        );
    }
}
