package akuma.whiplash.domains.alarm.presentation;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.*;
import static akuma.whiplash.domains.auth.exception.AuthErrorCode.*;
import static akuma.whiplash.domains.member.exception.MemberErrorCode.MEMBER_NOT_FOUND;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRemoveRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmCheckinResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmSyncResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import akuma.whiplash.domains.alarm.application.dto.response.GetAlarmsResponse;
import akuma.whiplash.domains.alarm.application.usecase.AlarmUseCase;
import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.global.annotation.swagger.CustomErrorCodes;
import akuma.whiplash.global.response.ApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
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
        CreateAlarmResponse response = alarmUseCase.createAlarm(request, memberContext.memberId());
        return ApplicationResponse.onSuccess(response);
    }

    @CustomErrorCodes(
        alarmErrorCodes = {ALARM_NOT_FOUND, ALARM_DELETE_NOT_AVAILABLE},
        authErrorCodes = {PERMISSION_DENIED}
    )
    @Operation(summary = "알람 삭제", description = "알람을 삭제합니다.")
    @DeleteMapping("/{alarmId}")
    public ApplicationResponse<Void> removeAlarm(
        @AuthenticationPrincipal MemberContext memberContext,
        @PathVariable Long alarmId,
        @RequestBody @Valid AlarmRemoveRequest request
    ) {
        alarmUseCase.removeAlarm(memberContext.memberId(), alarmId, request.reason());
        return ApplicationResponse.onSuccess();
    }

    @CustomErrorCodes(
        alarmErrorCodes = {
            ALARM_NOT_FOUND,
            ALARM_OCCURRENCE_NOT_FOUND,
            CHECKIN_OUT_OF_RANGE,
            ALREADY_DEACTIVATED,
            CHECKIN_NOT_YET_AVAILABLE
        },
        authErrorCodes = {PERMISSION_DENIED}
    )
    @Operation(summary = "알람 도착 인증", description = "알람 도착 인증을 합니다. 도착 위치 반경 50m 내에 들어와야 도착 인증이 가능합니다.")
    @PostMapping("/{alarmId}/checkin")
    public ApplicationResponse<AlarmCheckinResponse> checkin(
        @PathVariable Long alarmId,
        @RequestBody @Valid AlarmCheckinRequest request,
        @AuthenticationPrincipal MemberContext memberContext
    ) {
        return ApplicationResponse.onSuccess(alarmUseCase.checkinAlarm(memberContext.memberId(), alarmId, request));
    }

    @CustomErrorCodes(memberErrorCodes = {MEMBER_NOT_FOUND})
    @Operation(summary = "알람 목록 조회", description = "사용자가 등록한 알람 목록을 조회합니다.")
    @GetMapping
    public ApplicationResponse<GetAlarmsResponse> getAlarms(@AuthenticationPrincipal MemberContext memberContext) {
        return ApplicationResponse.onSuccess(alarmUseCase.getAlarms(memberContext.memberId()));
    }

    @CustomErrorCodes(memberErrorCodes = {MEMBER_NOT_FOUND})
    @Operation(summary = "알람 전체 동기화 조회", description = "서버 기준 알람 상태를 조회하여 로컬 알람과 동기화합니다.")
    @GetMapping("/sync")
    public ApplicationResponse<AlarmSyncResponse> syncAlarms(@AuthenticationPrincipal MemberContext memberContext) {
        return ApplicationResponse.onSuccess(alarmUseCase.getSyncAlarms(memberContext.memberId()));
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
        alarmUseCase.ringAlarm(memberContext.memberId(), alarmId);
        return ApplicationResponse.onSuccess();
    }
}
