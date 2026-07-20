package akuma.whiplash.domains.auth.application.utils;

import akuma.whiplash.domains.auth.application.dto.etc.KakaoUserInfo;
import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import akuma.whiplash.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component("KAKAO")
@RequiredArgsConstructor
public class KakaoVerifier implements SocialVerifier {

    private final WebClient webClient;

    @Value("${oauth.kakao.user-info-url:https://kapi.kakao.com/v2/user/me}")
    private String kakaoUserInfoUrl;

    @Override
    public SocialMemberInfo verify(SocialLoginRequest request) {
        KakaoUserInfo response = webClient.get()
            .uri(kakaoUserInfoUrl)
            .headers(h -> h.setBearerAuth(request.providerAccessToken()))
            .retrieve()
            .bodyToMono(KakaoUserInfo.class)
            .block();

        log.info("Kakao API response: {}", response);

        if (response == null || response.id() == null) {
            throw ApplicationException.from(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        return SocialMemberInfo.builder()
            .provider(SocialType.KAKAO)
            .providerUserId(String.valueOf(response.id()))
            .email(extractEmail(response))
            .name(extractNickname(response))
            .build();
    }

    private String extractEmail(KakaoUserInfo response) {
        if (response.kakaoAccount() == null) {
            return null;
        }
        return response.kakaoAccount().email();
    }

    private String extractNickname(KakaoUserInfo response) {
        if (response.properties() == null) {
            return null;
        }
        return response.properties().nickname();
    }
}
