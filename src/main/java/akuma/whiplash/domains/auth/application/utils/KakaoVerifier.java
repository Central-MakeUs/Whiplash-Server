package akuma.whiplash.domains.auth.application.utils;

import akuma.whiplash.domains.auth.application.dto.etc.KakaoUserInfo;
import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component("KAKAO")
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

        log.info("Kakao API response: {}", response);

        return SocialMemberInfo.builder()
            .socialId(SocialType.KAKAO.name() + "_" + String.valueOf(response.id()))
            .email(response.kakaoAccount().email())
            .name(response.properties().nickname())
            .build();
    }
}
