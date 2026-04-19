package akuma.whiplash.domains.device.domain.service;

import akuma.whiplash.domains.device.application.dto.request.FcmTokenUpdateRequest;
import akuma.whiplash.domains.device.application.dto.response.FcmTokenUpdateResponse;

public interface DeviceCommandService {

    FcmTokenUpdateResponse modifyFcmToken(Long memberId, FcmTokenUpdateRequest request);
}
