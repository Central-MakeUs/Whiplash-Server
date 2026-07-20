package akuma.whiplash.loadtest.application.usecase;

import akuma.whiplash.infrastructure.redis.RedisService;
import akuma.whiplash.loadtest.domain.util.LoadTestMemberHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 사례 1. FCM 대량 발송 최적화 — 부하 테스트 UseCase
 *
 * AS-IS: 토큰 1건씩 순차 전송 (Thread.sleep으로 FCM 왕복 비용 시뮬레이션)
 * TO-BE: 500건 배치 + 배치 단위 병렬 전송 (배치당 Thread.sleep 1회)
 *
 * 실제 Firebase 호출 없이 지연 시간만으로 처리량 차이를 수치화한다.
 */
@Profile("!prod")
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmBulkSendLoadTestUseCase {

    private static final int FCM_BATCH_LIMIT = 500;

    private final RedisService redisService;
    private final RedisTemplate<String, String> redisTemplate;
    private final LoadTestMemberHelper memberHelper;

    // ===== Response Records =====

    public record MemberInfo(Long memberId, String accessToken) {}

    public record SetupResponse(
        int memberCount,
        int totalTokens,
        List<MemberInfo> members,
        long durationMs
    ) {}

    public record SendResponse(
        String scenario,
        int tokenCount,
        int batchCount,
        int iterations,
        long totalMs,
        double avgMsPerBatch,
        long fcmLatencyMs
    ) {}

    // ===== Setup =====

    public SetupResponse setup(int memberCount, int tokensPerMember) {
        long start = System.currentTimeMillis();

        List<LoadTestMemberHelper.TestMemberInfo> members =
            memberHelper.createTestMembers("lt-fcm", memberCount);

        for (LoadTestMemberHelper.TestMemberInfo m : members) {
            for (int j = 0; j < tokensPerMember; j++) {
                String deviceId = "lt-dev-" + m.memberId() + "-" + j;
                String token = "lt-tok-" + m.memberId() + "-" + j;
                redisService.upsertFcmToken(m.memberId(), deviceId, token);
            }
        }

        List<MemberInfo> memberInfos = members.stream()
            .map(m -> new MemberInfo(m.memberId(), m.accessToken()))
            .toList();

        return new SetupResponse(memberCount, memberCount * tokensPerMember, memberInfos,
            System.currentTimeMillis() - start);
    }

    // ===== AS-IS: 단건 순차 전송 시뮬레이션 =====

    /**
     * 각 토큰에 대해 {@code fcmLatencyMs} 동안 sleep하여 HTTP/1.1 단건 요청 비용을 재현한다.
     * totalMs ≈ tokenCount × fcmLatencyMs
     */
    public SendResponse sendSequential(List<Long> memberIds, int iterations, long fcmLatencyMs) {
        List<String> allTokens = collectAllTokens(memberIds);
        int tokenCount = allTokens.size();

        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            for (String ignored : allTokens) {
                simulateFcmCall(fcmLatencyMs);
            }
        }
        long totalMs = System.currentTimeMillis() - start;

        int totalCalls = tokenCount * iterations;
        double avgMsPerCall = totalCalls > 0 ? (double) totalMs / totalCalls : 0;

        log.info("[FCM AS-IS] sequential: tokens={}, iterations={}, totalMs={}, avgMs={}",
            tokenCount, iterations, totalMs, avgMsPerCall);

        return new SendResponse("as-is-sequential", tokenCount, totalCalls, iterations, totalMs, avgMsPerCall, fcmLatencyMs);
    }

    // ===== TO-BE: 배치 전송 시뮬레이션 =====

    /**
     * 500건씩 묶어 배치당 {@code fcmLatencyMs} 동안 sleep하여 HTTP/2 멀티플렉싱 비용을 재현한다.
     * totalMs ≈ ceil(tokenCount / 500) × fcmLatencyMs
     */
    public SendResponse sendBatch(List<Long> memberIds, int iterations, long fcmLatencyMs) {
        List<String> allTokens = collectAllTokens(memberIds);
        int tokenCount = allTokens.size();
        int batchCount = (int) Math.ceil((double) tokenCount / FCM_BATCH_LIMIT);

        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            for (int b = 0; b < batchCount; b++) {
                simulateFcmCall(fcmLatencyMs);
            }
        }
        long totalMs = System.currentTimeMillis() - start;

        int totalBatches = batchCount * iterations;
        double avgMsPerBatch = totalBatches > 0 ? (double) totalMs / totalBatches : 0;

        log.info("[FCM TO-BE] batch: tokens={}, batchCount={}, iterations={}, totalMs={}, avgMsPerBatch={}",
            tokenCount, batchCount, iterations, totalMs, avgMsPerBatch);

        return new SendResponse("to-be-batch", tokenCount, totalBatches, iterations, totalMs, avgMsPerBatch, fcmLatencyMs);
    }

    // ===== Cleanup =====

    public void cleanup(List<Long> memberIds, int tokensPerMember) {
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
        log.info("[FCM Cleanup] members={}", memberIds.size());
    }

    // ===== Private Helpers =====

    private List<String> collectAllTokens(List<Long> memberIds) {
        List<String> tokens = new ArrayList<>();
        for (Long memberId : memberIds) {
            tokens.addAll(redisService.getFcmTokens(memberId));
        }
        return tokens;
    }

    private void simulateFcmCall(long latencyMs) {
        if (latencyMs <= 0) return;
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
