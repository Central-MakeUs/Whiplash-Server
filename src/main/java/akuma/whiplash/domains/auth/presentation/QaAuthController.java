package akuma.whiplash.domains.auth.presentation;

import akuma.whiplash.domains.member.domain.contants.Role;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import akuma.whiplash.global.response.ApplicationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * QA 전용 토큰 발급 컨트롤러
 *
 * - @Profile("qa") 로 QA 서버에서만 활성화, prod 배포 시 Bean 자체가 생성되지 않음
 * - 소셜 로그인 없이 provider + providerUserId 기반으로 JWT를 발급
 * - k6 setup() 단계에서 호출해 테스트용 토큰을 자동 발급받는 용도
 *
 * 사용 예시:
 *   POST /qa/auth/token?provider=GOOGLE&providerUserId=QA_001&deviceId=k6-device-001
 */
@Profile("qa")
@RestController
@RequiredArgsConstructor
@RequestMapping("/qa/auth")
public class QaAuthController {

    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    private static final String BEARER_PREFIX = "Bearer ";

    @PostMapping("/token")
    public ApplicationResponse<String> issueToken(
        @RequestParam String provider,
        @RequestParam String providerUserId,
        @RequestParam(defaultValue = "k6-device") String deviceId
    ) {
        SocialType socialType = SocialType.valueOf(provider.toUpperCase());
        MemberEntity member = memberRepository.findByProviderAndProviderUserId(socialType, providerUserId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원: provider=" + provider + ", providerUserId=" + providerUserId));

        String accessToken = jwtProvider.generateAccessToken(
            member.getId(),
            Role.USER,
            deviceId
        );

        return ApplicationResponse.onSuccess(BEARER_PREFIX + accessToken);
    }
}
