package akuma.whiplash.domains.auth.domain.service;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.application.dto.request.LogoutRequest;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.application.dto.response.LoginResponse;
import akuma.whiplash.domains.auth.application.dto.response.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthCommandService {

    LoginResponse login(SocialLoginRequest request);
    void logout(LogoutRequest request, Long memberId);
    TokenResponse reissueToken(MemberContext memberContext);
}
