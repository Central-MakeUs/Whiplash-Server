package akuma.whiplash.loadtest.domain.util;

import akuma.whiplash.domains.member.domain.contants.Role;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 부하 테스트용 임시 회원 생성/삭제 공통 유틸
 *
 * - socialId 패턴: "{prefix}-{runId}-{index}" 형태로 고유 식별
 * - runId는 UUID 앞 8자리를 사용하여 병렬 테스트 실행 간 충돌 방지
 */
@Profile("!prod")
@Component
@RequiredArgsConstructor
public class LoadTestMemberHelper {

    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    private static final String BEARER_PREFIX = "Bearer ";

    public record TestMemberInfo(Long memberId, String socialId, String accessToken) {}

    public List<TestMemberInfo> createTestMembers(String prefix, int count) {
        String runId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        List<TestMemberInfo> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String socialId = prefix + "-" + runId + "-" + i;
            MemberEntity member = MemberEntity.builder()
                .socialId(socialId)
                .email("lt-" + runId + "-" + i + "@test.com")
                .nickname("부하테스트" + i)
                .role(Role.USER)
                .privacyPolicy(true)
                .pushNotificationPolicy(true)
                .privacyAgreedAt(LocalDateTime.now())
                .pushAgreedAt(LocalDateTime.now())
                .build();
            member = memberRepository.save(member);

            String deviceId = "lt-dev-" + runId + "-" + i;
            String accessToken = BEARER_PREFIX + jwtProvider.generateAccessToken(member.getId(), Role.USER, deviceId);
            result.add(new TestMemberInfo(member.getId(), socialId, accessToken));
        }

        return result;
    }

    public void deleteTestMembers(List<Long> memberIds) {
        memberRepository.deleteAllById(memberIds);
    }
}
