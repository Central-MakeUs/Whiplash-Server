package akuma.whiplash.loadtest.application.usecase;

import akuma.whiplash.infrastructure.redis.RedisService;
import akuma.whiplash.loadtest.domain.util.LoadTestMemberHelper;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 사례 3. Redis 기반 다중 디바이스 FCM 토큰 관리 — 부하 테스트 UseCase
 *
 * AS-IS (register-non-atomic):
 *   GET → SREM → DEL → SET → SADD를 각각 독립 명령으로 실행한다.
 *   여러 스레드가 동시에 같은 deviceId의 토큰을 갱신하면 GET-SET 사이에 다른 요청이 끼어들어
 *   이전 토큰이 memberSet에 잔류할 수 있다 (stale token).
 *
 * TO-BE (register-atomic):
 *   기존 RedisService.upsertFcmToken()을 재사용한다.
 *   MULTI/EXEC으로 old token 정리 + new token 등록을 원자적으로 처리한다.
 *
 * 검증 방법:
 *   동일 memberId, 동일 deviceId에 대해 여러 VU가 동시에 다른 토큰을 등록한 뒤
 *   GET /api/load-test/fcm-token/verify/{memberId} 로 tokenCount를 확인한다.
 *   - AS-IS: tokenCount > 1 (stale token 잔류 가능)
 *   - TO-BE: tokenCount == 1 (항상 마지막 토큰만 유지)
 */
@Profile("!prod")
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTokenLoadTestUseCase {

    private final RedisService redisService;
    private final RedisTemplate<String, String> redisTemplate;
    private final LoadTestMemberHelper memberHelper;

    // ===== Response Records =====

    public record SetupResponse(
        int memberCount,
        List<MemberInfo> members,
        long durationMs
    ) {}

    public record MemberInfo(Long memberId, String accessToken) {}

    public record RegisterResponse(
        String scenario,
        Long memberId,
        String deviceId,
        String fcmToken,
        long durationMs
    ) {}

    public record VerifyResponse(
        Long memberId,
        int tokenCount,
        Set<String> tokens
    ) {}

    // ===== Setup =====

    public SetupResponse setup(int memberCount) {
        long start = System.currentTimeMillis();
        List<LoadTestMemberHelper.TestMemberInfo> members =
            memberHelper.createTestMembers("lt-token", memberCount);

        List<MemberInfo> memberInfos = members.stream()
            .map(m -> new MemberInfo(m.memberId(), m.accessToken()))
            .toList();

        return new SetupResponse(memberCount, memberInfos, System.currentTimeMillis() - start);
    }

    // ===== AS-IS: 비원자적 토큰 등록 =====

    /**
     * 5개의 Redis 명령을 개별 실행한다 (트랜잭션 없음).
     *
     * 취약 구간: GET 후 MULTI/EXEC 전까지 다른 요청이 개입 가능.
     * → 동시 갱신 시 oldToken이 memberSet에 잔류할 수 있다.
     */
    public RegisterResponse registerNonAtomic(Long memberId, String deviceId, String newToken) {
        long start = System.currentTimeMillis();

        String deviceKey = "fcm:device:" + deviceId + ":token";
        String memberSetKey = "fcm:member:" + memberId;
        String newTokenMapKey = "fcm:token:" + newToken + ":device";

        // 1) 이전 토큰 조회 (원자성 밖 — 취약 구간 시작)
        String oldToken = redisTemplate.opsForValue().get(deviceKey);

        // 2) 이전 토큰 정리: 각 명령이 독립 실행 → 중간에 다른 요청 개입 가능
        if (oldToken != null && !oldToken.isBlank()) {
            redisTemplate.opsForSet().remove(memberSetKey, oldToken);
            redisTemplate.delete("fcm:token:" + oldToken + ":device");
        }

        // 3) 신규 토큰 등록 (취약 구간 종료)
        redisTemplate.opsForValue().set(deviceKey, newToken);
        redisTemplate.opsForValue().set(newTokenMapKey, deviceId);
        redisTemplate.opsForSet().add(memberSetKey, newToken);

        return new RegisterResponse("as-is-non-atomic", memberId, deviceId, newToken,
            System.currentTimeMillis() - start);
    }

    // ===== TO-BE: MULTI/EXEC 원자적 토큰 등록 =====

    /**
     * RedisService.upsertFcmToken()을 재사용한다.
     * old token 정리 + new token 등록이 MULTI/EXEC 블록 내에서 원자적으로 처리된다.
     */
    public RegisterResponse registerAtomic(Long memberId, String deviceId, String newToken) {
        long start = System.currentTimeMillis();
        redisService.upsertFcmToken(memberId, deviceId, newToken);
        return new RegisterResponse("to-be-atomic", memberId, deviceId, newToken,
            System.currentTimeMillis() - start);
    }

    // ===== Verify =====

    public VerifyResponse verify(Long memberId) {
        Set<String> tokens = redisService.getFcmTokens(memberId);
        return new VerifyResponse(memberId, tokens.size(), tokens);
    }

    // ===== Cleanup =====

    public void cleanup(List<Long> memberIds) {
        for (Long memberId : memberIds) {
            Set<String> tokens = redisService.getFcmTokens(memberId);
            for (String token : tokens) {
                String deviceId = redisTemplate.opsForValue().get("fcm:token:" + token + ":device");
                if (deviceId != null) {
                    redisTemplate.delete("fcm:device:" + deviceId + ":token");
                }
                redisTemplate.delete("fcm:token:" + token + ":device");
            }
            redisTemplate.delete("fcm:member:" + memberId);
        }
        memberHelper.deleteTestMembers(memberIds);
        log.info("[Token Cleanup] members={}", memberIds.size());
    }
}
