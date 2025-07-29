package akuma.whiplash.domains.auth.application.utils;

import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.code.CommonErrorCode;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("APPLE")
@Slf4j
public class AppleVerifier implements SocialVerifier {

    @Value("${oauth.apple.client-id}")
    private String clientId;

    @Override
    public SocialMemberInfo verify(SocialLoginRequest request) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(request.token());
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // 1. aud(client_id) 검증
            String audience = claims.getAudience().get(0);
            if (!clientId.equals(audience)) {
                log.warn("Invalid audience");
                throw ApplicationException.from(CommonErrorCode.BAD_REQUEST);
            }

            // 2. nonce 검증
            String tokenNonce = claims.getStringClaim("nonce");
            String originalNonce = request.originalNonce();
            if (!StringUtils.hasText(originalNonce) && !originalNonce.equals(tokenNonce)) {
                log.warn("Invalid nonce: token={}, expected={}", tokenNonce, originalNonce);
                throw ApplicationException.from(CommonErrorCode.BAD_REQUEST);
            }

            String socialId = SocialType.APPLE.name() + "_" + claims.getSubject();
            String email = (String) claims.getClaim("email");
            String name = (String) claims.getClaim("name");

            log.info("Apple API user info: socialId={}, email={}, name={}", socialId, email, name);

            return SocialMemberInfo.builder()
                .socialId(SocialType.APPLE.name() + "_" + claims.getSubject())
                .email((String) claims.getClaim("email"))
                .name((String) claims.getClaim("name"))
                .build();

        } catch (Exception e) {
            log.warn("Apple token verification failed", e);
            throw ApplicationException.from(CommonErrorCode.BAD_REQUEST);
        }
    }
}
