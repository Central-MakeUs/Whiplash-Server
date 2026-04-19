package akuma.whiplash.domains.auth.application.utils;

import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("MOCK")
@Profile("!prod")
public class MockVerifier implements SocialVerifier {

    @Override
    public SocialMemberInfo verify(SocialLoginRequest request) {
        return SocialMemberInfo.builder()
            .provider(SocialType.MOCK)
            .providerUserId("123456789")
            .email("kmh@gmail.com")
            .name("김민형")
            .build();
    }
}
