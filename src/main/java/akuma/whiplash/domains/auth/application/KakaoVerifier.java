package akuma.whiplash.domains.auth.application;

import akuma.whiplash.domains.auth.application.dto.etc.KakaoUserInfo;
import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class KakaoVerifier implements SocialVerifier {

    private final WebClient webClient;

    @Override
    public SocialMemberInfo verify(String accessToken) {
        KakaoUserInfo response = webClient.get()
            .uri("https://kapi.kakao.com/v2/user/me") // baseUrl 사용하지 않아도 됨
            .headers(h -> h.setBearerAuth(accessToken))
            .retrieve()
            .bodyToMono(KakaoUserInfo.class)
            .block();


        return SocialMemberInfo.builder()
            .socialId(String.valueOf(response.id()))
            .socialType(SocialType.KAKAO)
            .email(response.kakaoAccount().email())
            .name(response.properties().nickname())
            .build();
    }
}
