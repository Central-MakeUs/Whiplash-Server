package akuma.whiplash.loadtest.application.usecase;

import akuma.whiplash.global.annotation.architecture.UseCase;
import akuma.whiplash.infrastructure.firebase.dto.FcmMetricResult;
import akuma.whiplash.loadtest.application.dto.request.TestPushRequest;
import akuma.whiplash.loadtest.application.dto.response.TestPushResponse;
import akuma.whiplash.loadtest.application.service.TestPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;

@Profile({"local", "qa"})
@UseCase
@RequiredArgsConstructor
public class TestPushUseCase {

    private final TestPushService testPushService;

    public TestPushResponse createTestPush(TestPushRequest request) {
        FcmMetricResult result = testPushService.createTestPush(request);
        return new TestPushResponse(result.getSuccessCount(), result.getFailedCount());
    }
}
