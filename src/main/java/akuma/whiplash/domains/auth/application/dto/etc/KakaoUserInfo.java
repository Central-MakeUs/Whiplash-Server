package akuma.whiplash.domains.auth.application.dto.etc;

public record KakaoUserInfo(
    Long id,
    Properties properties,
    KakaoAccount kakaoAccount
) {
    public record Properties(String nickname) {}
    public record KakaoAccount(String email) {}
}
