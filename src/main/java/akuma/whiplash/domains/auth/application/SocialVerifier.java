package akuma.whiplash.domains.auth.application;

import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;

public interface SocialVerifier {

    SocialMemberInfo verify(SocialLoginRequest request);
}
