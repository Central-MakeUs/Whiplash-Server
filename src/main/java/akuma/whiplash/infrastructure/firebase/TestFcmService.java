package akuma.whiplash.infrastructure.firebase;

import akuma.whiplash.infrastructure.firebase.dto.FcmMetricResult;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("qa")
public class TestFcmService {

    public FcmMetricResult createTestNotification(
        String token,
        String title,
        String body,
        String deeplink
    ) {
        MulticastMessage message = MulticastMessage.builder()
            .addToken(token)
            .putAllData(Map.of("title", title, "body", body, "deeplink", deeplink))
            .setAndroidConfig(AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setTtl(Duration.ofMinutes(5).toMillis())
                .build())
            .setApnsConfig(ApnsConfig.builder()
                .putHeader("apns-push-type", "alert")
                .putHeader("apns-priority", "10")
                .setAps(Aps.builder()
                    .setAlert(ApsAlert.builder().setTitle(title).setBody(body).build())
                    .setSound("default")
                    .build())
                .build())
            .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            return FcmMetricResult.builder()
                .successCount(response.getSuccessCount())
                .failedCount(response.getFailureCount())
                .build();
        } catch (FirebaseMessagingException e) {
            log.warn("테스트 FCM 전송 실패: error={}", e.getErrorCode());
            return FcmMetricResult.builder().successCount(0).failedCount(1).build();
        }
    }

}
