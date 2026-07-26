package akuma.whiplash.domains.alarm.presentation;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.*;
import static akuma.whiplash.domains.ad.exception.AdErrorCode.*;
import static akuma.whiplash.domains.auth.exception.AuthErrorCode.*;
import static akuma.whiplash.domains.device.exception.DeviceErrorCode.*;
import static akuma.whiplash.domains.member.exception.MemberErrorCode.MEMBER_NOT_FOUND;
import static akuma.whiplash.domains.payment.exception.PaymentErrorCode.*;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmAdSessionCreateRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmDeleteByAdRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmDeleteByPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmAdSessionCreateResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmCheckinResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmDeleteMethodResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmPaymentResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmSyncResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import akuma.whiplash.domains.alarm.application.dto.response.GetAlarmsResponse;
import akuma.whiplash.domains.alarm.application.usecase.AlarmUseCase;
import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.global.annotation.swagger.CustomErrorCodes;
import akuma.whiplash.global.response.ApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/alarms")
public class AlarmController {

    private final AlarmUseCase alarmUseCase;

    @CustomErrorCodes(
          memberErrorCodes = {MEMBER_NOT_FOUND}
        , alarmErrorCodes = {DUPLICATE_ALARM_PURPOSE}
    )
    @Operation(summary = "알람 등록", description = "사용자가 알람을 등록합니다.")
    @PostMapping
    public ApplicationResponse<CreateAlarmResponse> createAlarm(@AuthenticationPrincipal MemberContext memberContext, @RequestBody @Valid AlarmRegisterRequest request) {
        CreateAlarmResponse response = alarmUseCase.createAlarm(request, memberContext.memberId(), memberContext.deviceId());
        return ApplicationResponse.onSuccess(response);
    }

    @CustomErrorCodes(
        alarmErrorCodes = {ALARM_NOT_FOUND, TODAY_IS_NOT_ALARM_DAY, ALREADY_DEACTIVATED},
        paymentErrorCodes = {DUPLICATE_PAYMENT, PAYMENT_VERIFICATION_FAILED},
        deviceErrorCodes = {DEVICE_NOT_FOUND},
        authErrorCodes = {PERMISSION_DENIED}
    )
    @Operation(summary = "결제로 알람 삭제", description = "알람 당일 인앱 결제를 통해 알람을 삭제합니다.")
    @PostMapping("/{alarmId}/delete/payment")
    public ApplicationResponse<Void> removeAlarmByPayment(
        @AuthenticationPrincipal MemberContext memberContext,
        @PathVariable Long alarmId,
        @RequestBody @Valid AlarmDeleteByPaymentRequest request
    ) {
        alarmUseCase.removeAlarmByPayment(memberContext.memberId(), alarmId, request);
        return ApplicationResponse.onSuccess();
    }

    @CustomErrorCodes(
        alarmErrorCodes = {ALARM_NOT_FOUND, ALARM_DELETE_REQUIRES_PAYMENT},
        authErrorCodes = {PERMISSION_DENIED}
    )
    @Operation(summary = "광고 삭제 세션 발급", description = "AdMob 보상형 광고 표시 전에 custom_data로 사용할 광고 세션 ID를 발급합니다.")
    @PostMapping("/{alarmId}/ad-session")
    public ApplicationResponse<AlarmAdSessionCreateResponse> createAdSession(
        @AuthenticationPrincipal MemberContext memberContext,
        @PathVariable Long alarmId,
        @RequestBody @Valid AlarmAdSessionCreateRequest request
    ) {
        return ApplicationResponse.onSuccess(
            alarmUseCase.createAdSession(memberContext.memberId(), alarmId, request)
        );
    }

    @CustomErrorCodes(
        alarmErrorCodes = {ALARM_NOT_FOUND, ALARM_DELETE_REQUIRES_PAYMENT},
        adErrorCodes = {
            AD_SESSION_NOT_VERIFIED,
            AD_SESSION_EXPIRED,
            AD_SESSION_MISMATCH,
            AD_SESSION_NOT_FOUND,
            AD_SESSION_ALREADY_CONSUMED
        },
        authErrorCodes = {PERMISSION_DENIED}
    )
    @Operation(summary = "광고 시청으로 알람 삭제", description = "SSV 검증이 완료된 광고 세션을 제출하여 알람을 삭제합니다.")
    @PostMapping("/{alarmId}/delete/ad")
    public ApplicationResponse<Void> removeAlarmByAd(
        @AuthenticationPrincipal MemberContext memberContext,
        @PathVariable Long alarmId,
        @RequestBody @Valid AlarmDeleteByAdRequest request
    ) {
        alarmUseCase.removeAlarmByAd(memberContext.memberId(), alarmId, request);
        return ApplicationResponse.onSuccess();
    }

    @CustomErrorCodes(memberErrorCodes = {MEMBER_NOT_FOUND})
    @Operation(summary = "알람 목록 조회", description = "사용자가 등록한 알람 목록을 조회합니다.")
    @GetMapping
    public ApplicationResponse<GetAlarmsResponse> getAlarms(@AuthenticationPrincipal MemberContext memberContext) {
        return ApplicationResponse.onSuccess(alarmUseCase.getAlarms(memberContext.memberId(), memberContext.deviceId()));
    }

    @CustomErrorCodes(memberErrorCodes = {MEMBER_NOT_FOUND})
    @Operation(summary = "알람 전체 동기화 조회", description = "서버 기준 알람 상태를 조회하여 로컬 알람과 동기화합니다.")
    @GetMapping("/sync")
    public ApplicationResponse<AlarmSyncResponse> syncAlarms(@AuthenticationPrincipal MemberContext memberContext) {
        return ApplicationResponse.onSuccess(
            alarmUseCase.getSyncAlarms(memberContext.memberId(), memberContext.deviceId())
        );
    }

    @CustomErrorCodes(
        alarmErrorCodes = {ALARM_NOT_FOUND},
        authErrorCodes = {PERMISSION_DENIED}
    )
    @Operation(summary = "알람 삭제 방법 조회", description = "알람 삭제 전 광고 시청(AD) 또는 벌금 납부(PAYMENT) 중 어떤 방법이 필요한지 조회합니다.")
    @GetMapping("/{alarmId}/delete-method")
    public ApplicationResponse<AlarmDeleteMethodResponse> getAlarmDeleteMethod(
        @AuthenticationPrincipal MemberContext memberContext,
        @PathVariable @Positive(message = "METHOD_ARGUMENT_NOT_VALID") Long alarmId
    ) {
        return ApplicationResponse.onSuccess(
            alarmUseCase.getAlarmDeleteMethod(memberContext.memberId(), alarmId)
        );
    }

    @CustomErrorCodes(
        alarmErrorCodes = {
            ALARM_NOT_FOUND,
            ALARM_OCCURRENCE_NOT_FOUND,
            CHECKIN_OUT_OF_RANGE,
            ALREADY_DEACTIVATED,
            CHECKIN_NOT_YET_AVAILABLE,
            ALARM_LOCATION_RESELECTION_REQUIRED
        },
        authErrorCodes = {PERMISSION_DENIED}
    )
    @Operation(summary = "알람 도착 인증", description = "알람 도착 인증을 합니다. 도착 위치 반경 50m 내에 들어와야 도착 인증이 가능합니다.")
    @PostMapping("/{alarmId}/off/checkin")
    public ApplicationResponse<AlarmCheckinResponse> checkin(
        @PathVariable Long alarmId,
        @RequestBody @Valid AlarmCheckinRequest request,
        @AuthenticationPrincipal MemberContext memberContext
    ) {
        return ApplicationResponse.onSuccess(alarmUseCase.checkinAlarm(memberContext.memberId(), alarmId, request));
    }

    @CustomErrorCodes(
        alarmErrorCodes = {
            ALARM_NOT_FOUND,
            ALARM_OCCURRENCE_NOT_FOUND,
            ALREADY_DEACTIVATED
        },
        paymentErrorCodes = {
            DUPLICATE_PAYMENT,
            PAYMENT_NOT_YET_AVAILABLE,
            PAYMENT_VERIFICATION_FAILED
        },
        deviceErrorCodes = {DEVICE_NOT_FOUND},
        authErrorCodes = {PERMISSION_DENIED}
    )
    @Operation(summary = "결제로 알람 끄기", description = "인앱 결제를 통해 알람 회차를 비활성화합니다.")
    @PostMapping("/{alarmId}/off/payment")
    public ApplicationResponse<AlarmPaymentResponse> deactivateByPayment(
        @AuthenticationPrincipal MemberContext memberContext,
        @PathVariable Long alarmId,
        @RequestBody @Valid AlarmPaymentRequest request
    ) {
        return ApplicationResponse.onSuccess(
            alarmUseCase.deactivateByPayment(memberContext.memberId(), alarmId, request)
        );
    }

    @CustomErrorCodes(
        alarmErrorCodes = {ALARM_NOT_FOUND, ALARM_OCCURRENCE_NOT_FOUND, ALREADY_DEACTIVATED, NOT_ALARM_TIME},
        authErrorCodes = {PERMISSION_DENIED}
    )
    @Operation(summary = "알람 울림", description = "알람이 울릴 때 호출합니다.")
    @PostMapping("/{alarmId}/ring")
    public ApplicationResponse<Void> ringAlarm(
        @AuthenticationPrincipal MemberContext memberContext,
        @PathVariable Long alarmId
    ) {
        alarmUseCase.ringAlarm(memberContext.memberId(), alarmId, memberContext.deviceId());
        return ApplicationResponse.onSuccess();
    }
}
