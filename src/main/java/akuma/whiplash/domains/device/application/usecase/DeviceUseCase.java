package akuma.whiplash.domains.device.application.usecase;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.device.application.dto.request.FcmTokenUpdateRequest;
import akuma.whiplash.domains.device.application.dto.response.FcmTokenUpdateResponse;
import akuma.whiplash.domains.device.domain.service.DeviceCommandService;
import akuma.whiplash.global.annotation.architecture.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeviceUseCase {

    private final DeviceCommandService deviceCommandService;

    public FcmTokenUpdateResponse modifyFcmToken(MemberContext memberContext, FcmTokenUpdateRequest request) {
        return deviceCommandService.modifyFcmToken(memberContext.memberId(), request);
    }
}
