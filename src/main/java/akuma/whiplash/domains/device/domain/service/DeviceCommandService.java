package akuma.whiplash.domains.device.domain.service;

import akuma.whiplash.domains.device.application.dto.request.DeviceUpdateRequest;
import akuma.whiplash.domains.device.application.dto.response.DeviceUpdateResponse;

public interface DeviceCommandService {

    DeviceUpdateResponse modifyDevice(Long memberId, DeviceUpdateRequest request);
}
