package akuma.whiplash.infrastructure.firebase;

import akuma.whiplash.domains.alarm.application.dto.etc.PushTargetDto;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.infrastructure.redis.RedisService;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final RedisService redisService;
    private final AlarmOccurrenceRepository alarmOccurrenceRepository;

    /** idempotent: 같은 토큰 재등록이어도 안전하게 동작 */
    public void registerFcmToken(Long memberId, String deviceId, String fcmToken) {
        redisService.upsertFcmToken(memberId, deviceId, fcmToken);
    }

    public void sendBulkNotification(List<PushTargetDto> targets) {
        Map<String, List<PushTargetDto>> grouped =
            targets.stream().collect(Collectors.groupingBy(
                dto -> String.format("1시간 뒤 %s에서 알림이 울릴 예정이에요!", dto.address())
            ));

        for (Map.Entry<String, List<PushTargetDto>> entry : grouped.entrySet()) {
            String body = entry.getKey();
            List<PushTargetDto> group = entry.getValue();

            List<List<PushTargetDto>> batches = partition(group, 500);
            for (List<PushTargetDto> batch : batches) {
                sendMulticast(batch, body);
            }
        }
    }

    private void sendMulticast(List<PushTargetDto> batch, String body) {
        MulticastMessage message = MulticastMessage.builder()
            .addAllTokens(batch.stream().map(PushTargetDto::token).toList())
            .setNotification(Notification.builder()
                .setTitle("눈 떠")
                .setBody(body)
                .build())
            .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            handleSendResult(response.getResponses(), batch);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 전송 실패", e);
        }
    }

    private void handleSendResult(List<SendResponse> responses, List<PushTargetDto> batch) {
        for (int i = 0; i < responses.size(); i++) {
            SendResponse res = responses.get(i);
            PushTargetDto dto = batch.get(i);

            if (res.isSuccessful()) {
                // 푸시 성공 시 occurrence 테이블에 전송 완료 처리
                alarmOccurrenceRepository.findById(dto.occurrenceId())
                    .ifPresent(occ -> {
                        occ.updateReminderSent(true);
                        // @Transactional 사용하지 않기 때문에 더티 체킹 동작 X -> 반영을 위해 save 메서드 호출
                        alarmOccurrenceRepository.save(occ);
                    });
            } else {
                FirebaseMessagingException e = (FirebaseMessagingException) res.getException();
                log.warn("FCM 실패: token={}, error={}", dto.token(), e.getErrorCode());

                if (isTokenInvalid(e)) {
                    redisService.removeInvalidToken(dto.memberId(), dto.token());
                }
            }
        }
    }

    private boolean isTokenInvalid(FirebaseMessagingException e) {
        return List.of(
            "registration-token-not-registered",
            "invalid-argument",
            "unregistered",
            "messaging/invalid-registration-token"
        ).contains(e.getErrorCode());
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
