package akuma.whiplash.domains.alarm.presentation;

import static akuma.whiplash.domains.ad.exception.AdErrorCode.*;
import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.*;
import static akuma.whiplash.domains.auth.exception.AuthErrorCode.PERMISSION_DENIED;
import static akuma.whiplash.domains.member.exception.MemberErrorCode.MEMBER_NOT_FOUND;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmAdActionRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmOffAdSessionCreateRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmAdSessionCreateResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmCheckinResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmDeactivationResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmSyncResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import akuma.whiplash.domains.alarm.application.dto.response.GetAlarmsResponse;
import akuma.whiplash.domains.alarm.application.dto.response.LocationPreparationResponse;
import akuma.whiplash.domains.alarm.application.usecase.AlarmUseCase;
import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.global.annotation.swagger.CustomErrorCodes;
import akuma.whiplash.global.response.ApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/alarms")
public class AlarmController {

    private final AlarmUseCase alarmUseCase;

    @CustomErrorCodes(memberErrorCodes = {MEMBER_NOT_FOUND}, alarmErrorCodes = {DUPLICATE_ALARM_PURPOSE})
    @Operation(summary = "알람 등록", description = "사용자가 알람을 등록합니다.")
    @PostMapping
    public ApplicationResponse<CreateAlarmResponse> createAlarm(@AuthenticationPrincipal MemberContext memberContext, @RequestBody @Valid AlarmRegisterRequest request) {
        return ApplicationResponse.onSuccess(alarmUseCase.createAlarm(request, memberContext.memberId(), memberContext.deviceId()));
    }

    @CustomErrorCodes(alarmErrorCodes = {ALARM_NOT_FOUND, ALARM_OCCURRENCE_NOT_FOUND, ALREADY_DEACTIVATED, CHECKIN_NOT_YET_AVAILABLE}, authErrorCodes = {PERMISSION_DENIED})
    @Operation(summary = "광고 알람 끄기 세션 발급", description = "AdMob 보상형 광고 표시 전에 발생 건에 귀속된 세션 ID를 발급합니다.")
    @PostMapping("/{alarmId}/off/ad-session")
    public ApplicationResponse<AlarmAdSessionCreateResponse> createOffAdSession(@AuthenticationPrincipal MemberContext memberContext, @PathVariable Long alarmId, @RequestBody @Valid AlarmOffAdSessionCreateRequest request) {
        return ApplicationResponse.onSuccess(alarmUseCase.createOffAdSession(memberContext.memberId(), memberContext.deviceId(), alarmId, request));
    }

    @CustomErrorCodes(alarmErrorCodes = {ALARM_NOT_FOUND, ALARM_OCCURRENCE_NOT_FOUND, ALREADY_DEACTIVATED, CHECKIN_NOT_YET_AVAILABLE}, adErrorCodes = {AD_SESSION_NOT_VERIFIED, AD_SESSION_EXPIRED, AD_SESSION_MISMATCH, AD_SESSION_NOT_FOUND, AD_SESSION_ALREADY_CONSUMED}, authErrorCodes = {PERMISSION_DENIED})
    @Operation(summary = "광고 시청으로 알람 끄기", description = "SSV 검증이 완료된 광고 세션으로 발급 때 지정한 발생 건을 비활성화합니다.")
    @PostMapping("/{alarmId}/off/ad")
    public ApplicationResponse<AlarmDeactivationResponse> deactivateByAd(@AuthenticationPrincipal MemberContext memberContext, @PathVariable Long alarmId, @RequestBody @Valid AlarmAdActionRequest request) {
        return ApplicationResponse.onSuccess(alarmUseCase.deactivateByAd(memberContext.memberId(), memberContext.deviceId(), alarmId, request));
    }

    @CustomErrorCodes(alarmErrorCodes = {ALARM_NOT_FOUND}, authErrorCodes = {PERMISSION_DENIED})
    @Operation(summary = "광고 알람 삭제 세션 발급", description = "AdMob 보상형 광고 표시 전에 삭제용 세션 ID를 발급합니다.")
    @PostMapping("/{alarmId}/delete/ad-session")
    public ApplicationResponse<AlarmAdSessionCreateResponse> createDeleteAdSession(@AuthenticationPrincipal MemberContext memberContext, @PathVariable Long alarmId) {
        return ApplicationResponse.onSuccess(alarmUseCase.createDeleteAdSession(memberContext.memberId(), memberContext.deviceId(), alarmId));
    }

    @CustomErrorCodes(alarmErrorCodes = {ALARM_NOT_FOUND}, adErrorCodes = {AD_SESSION_NOT_VERIFIED, AD_SESSION_EXPIRED, AD_SESSION_MISMATCH, AD_SESSION_NOT_FOUND, AD_SESSION_ALREADY_CONSUMED}, authErrorCodes = {PERMISSION_DENIED})
    @Operation(summary = "광고 시청으로 알람 삭제", description = "SSV 검증이 완료된 광고 세션으로 알람을 삭제합니다.")
    @PostMapping("/{alarmId}/delete/ad")
    public ApplicationResponse<Void> removeAlarmByAd(@AuthenticationPrincipal MemberContext memberContext, @PathVariable Long alarmId, @RequestBody @Valid AlarmAdActionRequest request) {
        alarmUseCase.removeAlarmByAd(memberContext.memberId(), memberContext.deviceId(), alarmId, request);
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
        return ApplicationResponse.onSuccess(alarmUseCase.getSyncAlarms(memberContext.memberId(), memberContext.deviceId()));
    }

    @CustomErrorCodes(alarmErrorCodes = {ALARM_NOT_FOUND, ALARM_OCCURRENCE_NOT_FOUND, ALREADY_DEACTIVATED, CHECKIN_NOT_YET_AVAILABLE}, authErrorCodes = {PERMISSION_DENIED})
    @Operation(summary = "알람 목적지 조회", description = "인증 화면에 표시할 알람 목적지 정보를 반환합니다.")
    @PostMapping("/{alarmId}/occurrences/{occurrenceId}/destination")
    public ApplicationResponse<LocationPreparationResponse> getLocationPreparation(@AuthenticationPrincipal MemberContext memberContext, @PathVariable @Positive Long alarmId, @PathVariable @Positive Long occurrenceId) {
        return ApplicationResponse.onSuccess(alarmUseCase.getLocationPreparation(memberContext.memberId(), alarmId, occurrenceId));
    }

    @CustomErrorCodes(alarmErrorCodes = {ALARM_NOT_FOUND, ALARM_OCCURRENCE_NOT_FOUND, CHECKIN_OUT_OF_RANGE, ALREADY_DEACTIVATED, CHECKIN_NOT_YET_AVAILABLE, ALARM_LOCATION_NOT_READY}, authErrorCodes = {PERMISSION_DENIED})
    @Operation(summary = "알람 도착 인증", description = "알람 도착 인증을 합니다.")
    @PostMapping("/{alarmId}/off/checkin")
    public ApplicationResponse<AlarmCheckinResponse> checkin(@PathVariable Long alarmId, @RequestBody @Valid AlarmCheckinRequest request, @AuthenticationPrincipal MemberContext memberContext) {
        return ApplicationResponse.onSuccess(alarmUseCase.checkinAlarm(memberContext.memberId(), alarmId, request));
    }

    @CustomErrorCodes(alarmErrorCodes = {ALARM_NOT_FOUND, ALARM_OCCURRENCE_NOT_FOUND, ALREADY_DEACTIVATED, NOT_ALARM_TIME}, authErrorCodes = {PERMISSION_DENIED})
    @Operation(summary = "알람 울림", description = "알람이 울릴 때 호출합니다.")
    @PostMapping("/{alarmId}/ring")
    public ApplicationResponse<Void> ringAlarm(@AuthenticationPrincipal MemberContext memberContext, @PathVariable Long alarmId) {
        alarmUseCase.ringAlarm(memberContext.memberId(), alarmId, memberContext.deviceId());
        return ApplicationResponse.onSuccess();
    }
}
