package akuma.whiplash.infrastructure.firebase;

import akuma.whiplash.domains.alarm.application.dto.etc.PushTargetDto;
import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushTargetDto;
import akuma.whiplash.infrastructure.firebase.dto.FcmMetricResult;
import akuma.whiplash.infrastructure.firebase.dto.FcmSendResult;
import akuma.whiplash.infrastructure.redis.RedisService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("local")
@Primary
public class MockFcmService extends FcmService {

    public MockFcmService(RedisService redisService) {
        super(redisService);
    }

    @Override
    public FcmSendResult sendBulkNotification(List<PushTargetDto> targets) {
        log.info("[MockFcmService] sendBulkNotification called with {} targets", targets.size());

        if (targets == null || targets.isEmpty()) {
            return FcmSendResult.builder()
                .successOccurrenceIds(Set.of())
                .invalidTokens(List.of())
                .memberToTokens(Map.of())
                .successCount(0)
                .failedCount(0)
                .build();
        }

        Set<Long> successOccurrenceIds = new HashSet<>();
        Map<Long, List<String>> memberToTokens = new HashMap<>();

        for (PushTargetDto dto : targets) {
            successOccurrenceIds.add(dto.occurrenceId());
            memberToTokens.computeIfAbsent(dto.memberId(), k -> new ArrayList<>()).add(dto.token());
        }

        return FcmSendResult.builder()
            .successOccurrenceIds(successOccurrenceIds)
            .invalidTokens(new ArrayList<>())
            .memberToTokens(memberToTokens)
            .successCount(targets.size())
            .failedCount(0)
            .build();
    }

    @Override
    public FcmMetricResult sendRingingNotifications(List<RingingPushTargetDto> targets) {
        // 부모의 지연 로직(TEST_DELAY_MS)을 반영하기 위해 sleep 시뮬레이션 추가
        if (TEST_DELAY_MS > 0) {
            try {
                Thread.sleep(TEST_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("[MockFcmService] sendRingingNotifications called with {} targets", targets.size());

        if (targets == null || targets.isEmpty()) {
            return FcmMetricResult.builder()
                .successCount(0)
                .failedCount(0)
                .build();
        }

        int success = 0;
        int failed = 0;

        // "INVALID"로 시작하는 토큰은 실패로 간주
        for (RingingPushTargetDto target : targets) {
            if (target.token().startsWith("INVALID")) {
                failed++;
                // 실제 서비스에서는 여기서 Redis 삭제 로직이 호출되도록 유도해야 함
                // Mock에서는 카운트만 집계하거나, 필요 시 부모의 핸들링 로직을 흉내내야 함
            } else {
                success++;
            }
        }

        return FcmMetricResult.builder()
            .successCount(success)
            .failedCount(failed)
            .build();
    }
}
