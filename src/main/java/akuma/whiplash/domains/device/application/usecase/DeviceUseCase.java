package akuma.whiplash.domains.device.application.usecase;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.device.application.dto.request.DeviceUpdateRequest;
import akuma.whiplash.domains.device.application.dto.response.DeviceUpdateResponse;
import akuma.whiplash.domains.device.domain.service.DeviceCommandService;
import akuma.whiplash.global.annotation.architecture.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeviceUseCase {

    private final DeviceCommandService deviceCommandService;

    public DeviceUpdateResponse modifyDevice(MemberContext memberContext, DeviceUpdateRequest request) {
        return deviceCommandService.modifyDevice(memberContext.memberId(), request);
    }
}
