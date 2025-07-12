package akuma.whiplash.domains.auth.presentation;

import static akuma.whiplash.global.response.code.CommonErrorCode.*;

import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.application.dto.response.LoginResponse;
import akuma.whiplash.domains.auth.application.usecase.AuthUseCase;
import akuma.whiplash.global.annotation.swagger.CustomErrorCodes;
import akuma.whiplash.global.response.ApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
