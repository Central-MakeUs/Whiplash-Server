package akuma.whiplash.domains.auth.application.dto.etc;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfo(
    Long id,
    Properties properties,
    @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {
    public record Properties(String nickname) {}
    public record KakaoAccount(String email) {}
}
