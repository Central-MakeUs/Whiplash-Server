package akuma.whiplash.domains.auth.application.utils;

import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.member.application.utils.NicknameGenerator;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.code.CommonErrorCode;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("APPLE")
@Slf4j
@RequiredArgsConstructor
public class AppleVerifier implements SocialVerifier {

    @Value("${oauth.apple.client-id}")
    private String clientId;

    private final NicknameGenerator nicknameGenerator;

    @Override
    public SocialMemberInfo verify(SocialLoginRequest request) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(request.providerAccessToken());
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // 1. aud(client_id) 검증
            String audience = claims.getAudience().get(0);
            if (!clientId.equals(audience)) {
                log.warn("Invalid audience");
                throw ApplicationException.from(CommonErrorCode.BAD_REQUEST);
            }

            String providerUserId = claims.getSubject();
            String email = (String) claims.getClaim("email");

            // 애플은 닉네임 제공하지 않으므로 랜덤 생성
            String nickname = nicknameGenerator.generate();

            log.info("Apple API user info: providerUserId={}, email={}, name={}", providerUserId, email, nickname);

            return SocialMemberInfo.builder()
                .provider(SocialType.APPLE)
                .providerUserId(providerUserId)
                .email(email)
                .name(nickname)
                .build();

        } catch (Exception e) {
            log.warn("Apple token verification failed", e);
            throw ApplicationException.from(CommonErrorCode.BAD_REQUEST);
        }
    }
}
