package akuma.whiplash.domains.auth.domain.service;

import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.application.dto.response.LoginResponse;

public interface AuthCommandService {

    LoginResponse login(SocialLoginRequest request);
}
