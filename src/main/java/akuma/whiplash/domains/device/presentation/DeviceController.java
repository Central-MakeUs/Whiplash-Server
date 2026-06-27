package akuma.whiplash.domains.device.presentation;

import static akuma.whiplash.domains.device.exception.DeviceErrorCode.DEVICE_NOT_FOUND;
import static akuma.whiplash.global.response.code.CommonErrorCode.BAD_REQUEST;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.device.application.dto.request.DeviceUpdateRequest;
import akuma.whiplash.domains.device.application.dto.response.DeviceUpdateResponse;
import akuma.whiplash.domains.device.application.usecase.DeviceUseCase;
import akuma.whiplash.global.annotation.swagger.CustomErrorCodes;
import akuma.whiplash.global.response.ApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceUseCase deviceUseCase;

    @CustomErrorCodes(
        commonErrorCodes = {BAD_REQUEST},
        deviceErrorCodes = {DEVICE_NOT_FOUND}
    )
    @Operation(summary = "기기 정보 갱신", description = "현재 기기의 플랫폼, FCM 토큰, 앱/OS 버전, IANA time zone을 갱신합니다.")
    @PutMapping("/me")
    public ApplicationResponse<DeviceUpdateResponse> modifyDevice(
        @AuthenticationPrincipal MemberContext memberContext,
        @RequestBody @Valid DeviceUpdateRequest request
    ) {
        DeviceUpdateResponse response = deviceUseCase.modifyDevice(memberContext, request);
        return ApplicationResponse.onSuccess(response);
    }
}
