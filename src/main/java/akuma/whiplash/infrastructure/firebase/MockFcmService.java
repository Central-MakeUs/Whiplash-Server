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
        log.info("[MockFcmService] sendRingingNotifications called with {} targets", targets.size());

        if (targets == null || targets.isEmpty()) {
            return FcmMetricResult.builder()
                .successCount(0)
                .failedCount(0)
                .build();
        }

        return FcmMetricResult.builder()
            .successCount(targets.size())
            .failedCount(0)
            .build();
    }
}
