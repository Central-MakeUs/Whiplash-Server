package akuma.whiplash.infrastructure.firebase;

import akuma.whiplash.infrastructure.firebase.dto.FcmMetricResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
@Profile("local")
public class MockTestFcmService extends TestFcmService {

    @Override
    public FcmMetricResult createTestNotification(String token, String title, String body, String deeplink) {
        log.info("[MockTestFcmService] createTestNotification called");
        return FcmMetricResult.builder()
            .successCount(token.startsWith("INVALID") ? 0 : 1)
            .failedCount(token.startsWith("INVALID") ? 1 : 0)
            .build();
    }
}
