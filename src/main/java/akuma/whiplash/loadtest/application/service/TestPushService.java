package akuma.whiplash.loadtest.application.service;

import akuma.whiplash.loadtest.application.dto.request.TestPushRequest;
import akuma.whiplash.infrastructure.firebase.dto.FcmMetricResult;

public interface TestPushService {

    FcmMetricResult createTestPush(TestPushRequest request);
}
