package akuma.whiplash.domains.auth.application;

import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.code.CommonErrorCode;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AppleVerifier implements SocialVerifier {

    @Value("${oauth.apple.client-id}")
    private String clientId;

    @Override
    public SocialMemberInfo verify(String idToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(idToken);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            String audience = claims.getAudience().get(0);
            if (!clientId.equals(audience)) {
                log.warn("Invalid audience");
                throw ApplicationException.from(CommonErrorCode.BAD_REQUEST);
            }

            return SocialMemberInfo.builder()
                .socialId(claims.getSubject())
                .email((String) claims.getClaim("email"))
                .name((String) claims.getClaim("name"))
                .socialType(SocialType.APPLE)
                .build();

        } catch (Exception e) {
            log.warn("Apple token verification failed", e);
            throw ApplicationException.from(CommonErrorCode.BAD_REQUEST);
        }
    }
}
