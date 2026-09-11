package akuma.whiplash.global.config.security;

import akuma.whiplash.domains.auth.application.dto.response.TokenResponse;
import akuma.whiplash.domains.member.domain.contants.Role;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import akuma.whiplash.global.response.ApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/auth")
@RequiredArgsConstructor
@Profile("local")
public class DevAuthController {

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    @Operation(summary = "개발용 로그인 (Mock Only)", description = "임의의 memberId로 토큰을 발급받습니다. 존재하지 않는 회원이면 생성합니다.")
    @PostMapping("/login")
    @Transactional
    public ApplicationResponse<TokenResponse> devLogin(
        @RequestParam(defaultValue = "1") Long memberId,
        @RequestParam(defaultValue = "test-device-id") String deviceId) {
        
        MemberEntity member = memberRepository.findById(memberId)
            .orElseGet(() -> {
                MemberEntity newMember = MemberEntity.builder()
                    .email("test" + memberId + "@example.com")
                    .role(Role.USER)
                    .build();
                return memberRepository.save(newMember);
            });

        String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), deviceId);
        String refreshToken = jwtProvider.generateRefreshToken(member.getId(), deviceId, member.getRole());

        return ApplicationResponse.onSuccess(new TokenResponse(accessToken, refreshToken));
    }

}
