package akuma.whiplash.domains.auth.application;

import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;

public interface SocialVerifier {

    SocialMemberInfo verify(String token);
}
