package akuma.whiplash.domains.auth.application.utils;

import akuma.whiplash.domains.auth.application.dto.etc.KakaoUserInfo;
import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class KakaoVerifier implements SocialVerifier {

    private final WebClient webClient;

    @Override
    public SocialMemberInfo verify(SocialLoginRequest request) {
        KakaoUserInfo response = webClient.get()
            .uri("https://kapi.kakao.com/v2/user/me")
            .headers(h -> h.setBearerAuth(request.token()))
            .retrieve()
            .bodyToMono(KakaoUserInfo.class)
            .block();

        return SocialMemberInfo.builder()
            .socialId(SocialType.KAKAO.name() + "_" + String.valueOf(response.id()))
            .email(response.kakaoAccount().email())
            .name(response.properties().nickname())
            .build();
    }
}
