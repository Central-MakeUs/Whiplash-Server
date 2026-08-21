package akuma.whiplash.loadtest.application.service;

import akuma.whiplash.infrastructure.firebase.TestFcmService;
import akuma.whiplash.infrastructure.firebase.dto.FcmMetricResult;
import akuma.whiplash.loadtest.application.dto.request.TestPushRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile({"local", "qa"})
@Service
@RequiredArgsConstructor
public class TestPushServiceImpl implements TestPushService {

    private final TestFcmService testFcmService;

    @Override
    public FcmMetricResult createTestPush(TestPushRequest request) {
        return testFcmService.createTestNotification(
            request.fcmToken(), request.title(), request.body(), request.deeplink());
    }
}
