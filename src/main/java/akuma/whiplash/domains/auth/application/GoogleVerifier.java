package akuma.whiplash.domains.auth.application;

import akuma.whiplash.domains.auth.application.dto.etc.SocialMemberInfo;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.code.CommonErrorCode;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GoogleVerifier implements SocialVerifier{

    @Value("${oauth.google.client-id}")
    private String clientId;

    private final GoogleIdTokenVerifier verifier;

    public GoogleVerifier() {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
        .setAudience(List.of(clientId))
        .build();
    }

    @Override
    public SocialMemberInfo verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                log.warn("Google idToken verification failed");
                throw ApplicationException.from(CommonErrorCode.BAD_REQUEST);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            return SocialMemberInfo.builder()
                .socialId(payload.getSubject())
                .email(payload.getEmail())
                .name((String) payload.get("name"))
                .socialType(SocialType.GOOGLE)
                .build();
        } catch (Exception e) {
            log.warn("Google idToken verification failed", e);
            throw ApplicationException.from(CommonErrorCode.BAD_REQUEST);
        }
    }
}