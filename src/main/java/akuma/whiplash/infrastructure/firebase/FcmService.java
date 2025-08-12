package akuma.whiplash.infrastructure.firebase;

import akuma.whiplash.domains.alarm.application.dto.etc.PushTargetDto;
import akuma.whiplash.infrastructure.firebase.dto.FcmSendResult;
import akuma.whiplash.infrastructure.redis.RedisService;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidConfig.Priority;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private static final int FCM_MULTICAST_LIMIT = 500;
    private static final String DEFAULT_TITLE = "눈 떠";

    private final RedisService redisService;

    /**
     * idempotent: 같은 토큰 재등록이어도 안전하게 동작
     */
    public void registerFcmToken(Long memberId, String deviceId, String fcmToken) {
        redisService.upsertFcmToken(memberId, deviceId, fcmToken);
    }

    /**
     * 데이터 전용(data-only) 멀티캐스트 전송
     * - Notification payload 제거, data만 사용
     * - Android priority=HIGH, iOS apns-priority=10 + content-available=1
     * - 같은 body 문구(=주소)끼리 묶어서 전송 효율화
     * - 전송 성공한 occurrenceId 수집, 무효 토큰은 즉시 Redis에서 제거
     */
    public FcmSendResult sendBulkNotification(List<PushTargetDto> targets) {
        if (targets == null || targets.isEmpty()) {
            return FcmSendResult.builder()
                .successOccurrenceIds(Set.of())
                .invalidTokens(List.of())
                .memberToTokens(Map.of())
                .build();
        }

        // body 문구가 동일한 것끼리 묶어서 멀티캐스트 효율 증가
        Map<String, List<PushTargetDto>> groupedByBody = targets.stream()
            .collect(Collectors.groupingBy(
                dto -> String.format("1시간 뒤 %s에서 알림이 울릴 예정이에요!", dto.address())
            ));

        Set<Long> successOccurrenceIds = new HashSet<>();
        List<String> invalidTokens = new ArrayList<>();
        Map<Long, List<String>> memberToTokens = new HashMap<>();

        for (Map.Entry<String, List<PushTargetDto>> entry : groupedByBody.entrySet()) {
            String body = entry.getKey();
            List<PushTargetDto> group = dedupByToken(entry.getValue());

            Map<String, String> data = Map.of(
                "title", DEFAULT_TITLE,
                "body", body
            );

            for (List<PushTargetDto> batch : partition(group, FCM_MULTICAST_LIMIT)) {
                MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(batch.stream().map(PushTargetDto::token).toList())
                    .putAllData(data)
                    .setAndroidConfig(
                        buildAndroidConfig(Duration.ofMinutes(60), Priority.HIGH)
                    )
                    .setApnsConfig( // IOS 설정
                        buildApnsConfigAlert(DEFAULT_TITLE, body, Duration.ofMinutes(60))
                    )
                    .build();

                try {
                    BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                    log.info("FCM 멀티캐스트 결과: success={}, failure={}", response.getSuccessCount(), response.getFailureCount());

                    handleSendResult(response.getResponses(), batch, successOccurrenceIds, invalidTokens, memberToTokens);
                } catch (FirebaseMessagingException e) {
                    log.error("FCM 전송 실패(멀티캐스트 전체). body={}", body, e);
                }
            }
        }

        return FcmSendResult.builder()
            .successOccurrenceIds(successOccurrenceIds)
            .invalidTokens(invalidTokens)
            .memberToTokens(memberToTokens)
            .build();
    }

    /**
     * FCM 배치 전송 결과 집계
     */
    private void handleSendResult(
        List<SendResponse> responses,
        List<PushTargetDto> batch,
        Set<Long> successOccurrenceIds,
        List<String> invalidTokens,
        Map<Long, List<String>> memberToTokens
    ) {
        for (int i = 0; i < responses.size(); i++) {
            SendResponse res = responses.get(i);
            PushTargetDto dto = batch.get(i);

            if (res.isSuccessful()) {
                successOccurrenceIds.add(dto.occurrenceId());
                memberToTokens.computeIfAbsent(dto.memberId(),
                    k -> new ArrayList<>()
                ).add(dto.token());
            } else {
                Exception ex = res.getException();
                FirebaseMessagingException fme = (ex instanceof FirebaseMessagingException) ? (FirebaseMessagingException) ex : null;

                if (fme != null) {
                    log.warn("FCM 실패: token={}, error={}", dto.token(), fme.getErrorCode());
                    if (isTokenInvalid(fme)) {
                        invalidTokens.add(dto.token());
                    }
                } else {
                    log.warn("FCM 실패(원인 미상): token={}, ex={}", dto.token(), ex != null ? ex.getClass().getSimpleName() : "null");
                }
            }
        }
    }

    private AndroidConfig buildAndroidConfig(Duration ttl, Priority priority) {
        AndroidConfig.Builder builder = AndroidConfig.builder().setPriority(priority);
        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            builder.setTtl(ttl.toMillis());
        }
        return builder.build();
    }

    // iOS 알림(백그라운드 알림 X, UI에 표시되는 알림)
    private ApnsConfig buildApnsConfigAlert(String title, String body, Duration ttl) {
        ApnsConfig.Builder apns = ApnsConfig.builder()
            .putHeader("apns-push-type", "alert")
            .putHeader("apns-priority", "10")
            .setAps(Aps.builder()
                .setAlert(ApsAlert.builder()
                    .setTitle(title != null ? title : DEFAULT_TITLE)
                    .setBody(body)
                    .build())
                .setSound("default") // IOS 기본 알림 소리
                .build());

        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            long epochSec = (System.currentTimeMillis() + ttl.toMillis()) / 1000;
            apns.putHeader("apns-expiration", String.valueOf(epochSec));
        }
        return apns.build();
    }

    private boolean isTokenInvalid(FirebaseMessagingException e) {
        return List.of(
            "registration-token-not-registered",
            "invalid-argument",
            "unregistered",
            "messaging/invalid-registration-token"
        ).contains(e.getErrorCode());
    }

    // 같은 FCM 토큰 중복 제거
    private List<PushTargetDto> dedupByToken(List<PushTargetDto> src) {
        Map<String, PushTargetDto> map = new LinkedHashMap<>();
        for (PushTargetDto d : src) {
            map.putIfAbsent(d.token(), d);
        }
        return new ArrayList<>(map.values());
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
