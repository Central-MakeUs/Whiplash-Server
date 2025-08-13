package akuma.whiplash.domains.alarm.presentation;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.*;
import static akuma.whiplash.domains.auth.exception.AuthErrorCode.*;
import static akuma.whiplash.domains.member.exception.MemberErrorCode.MEMBER_NOT_FOUND;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmOffRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRemoveRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmInfoPreviewResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmOffResultResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmRemainingOffCountResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import akuma.whiplash.domains.alarm.application.usecase.AlarmUseCase;
import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.global.annotation.swagger.CustomErrorCodes;
import akuma.whiplash.global.response.ApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/alarms")
public class AlarmController {

    private final AlarmUseCase alarmUseCase;

    @CustomErrorCodes(memberErrorCodes = {MEMBER_NOT_FOUND})
    @Operation(summary = "알람 등록", description = "사용자가 알람을 등록합니다.")
    @PostMapping
    public ApplicationResponse<CreateAlarmResponse> createAlarm(@AuthenticationPrincipal MemberContext memberContext, @RequestBody @Valid AlarmRegisterRequest request) {
        CreateAlarmResponse response = alarmUseCase.createAlarm(request, memberContext.memberId());
        return ApplicationResponse.onSuccess(response);
    }

    // @CustomErrorCodes(
    //     alarmErrorCodes = {ALARM_NOT_FOUND, TODAY_IS_NOT_ALARM_DAY, ALREADY_OCCURRED_EXISTS},
    //     authErrorCodes = {PERMISSION_DENIED}
    // )
    // @Operation(summary = "알람 발생 내역 생성", description = "오늘 울려야할 알람이 처음 울렸을 때 호출하는 API입니다. 알람당 하루에 발생 내역은 1개만 생성할 수 있습니다.")
    // @PostMapping("/{alarmId}/occurrences")
    // public ApplicationResponse<CreateAlarmOccurrenceResponse> createAlarmOccurrence(@AuthenticationPrincipal MemberContext memberContext, @PathVariable Long alarmId) {
    //     CreateAlarmOccurrenceResponse response = alarmUseCase.createAlarmOccurrence(memberContext.memberId(), alarmId);
    //     return ApplicationResponse.onSuccess(response);
    // }

    @CustomErrorCodes(
        memberErrorCodes = {MEMBER_NOT_FOUND},
        alarmErrorCodes = {ALARM_NOT_FOUND, ALARM_OFF_LIMIT_EXCEEDED, ALREADY_DEACTIVATED, INVALID_CLIENT_DATE},
        authErrorCodes = {PERMISSION_DENIED}
    )
    @Operation(summary = "알람 끄기", description = "알람을 끕니다.")
    @PostMapping("/{alarmId}/off")
    public ApplicationResponse<AlarmOffResultResponse> alarmOff(@AuthenticationPrincipal MemberContext memberContext, @PathVariable Long alarmId, @RequestBody @Valid AlarmOffRequest request) {
        AlarmOffResultResponse response = alarmUseCase.alarmOff(memberContext.memberId(), alarmId, request.clientNow());
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
        alarmErrorCodes = {ALARM_NOT_FOUND, ALARM_OCCURRENCE_NOT_FOUND, CHECKIN_OUT_OF_RANGE, ALREADY_DEACTIVATED},
        authErrorCodes = {PERMISSION_DENIED}
    )
    @Operation(summary = "알람 도착 인증", description = "알람 도착 인증을 합니다. 도착 위치 반경 100m 내에 들어와야 도착 인증이 가능합니다.")
    @PostMapping("/{alarmId}/checkin")
    public ApplicationResponse<Void> checkin(
        @PathVariable Long alarmId,
        @RequestBody @Valid AlarmCheckinRequest request,
        @AuthenticationPrincipal MemberContext memberContext
    ) {
        alarmUseCase.checkinAlarm(memberContext.memberId(), alarmId, request);
        return ApplicationResponse.onSuccess();
    }

    @CustomErrorCodes(memberErrorCodes = {MEMBER_NOT_FOUND})
    @Operation(summary = "알람 목록 조회", description = "사용자가 등록한 알람 목록을 조회합니다.")
    @GetMapping
    public ApplicationResponse<List<AlarmInfoPreviewResponse>> getAlarms(@AuthenticationPrincipal MemberContext memberContext) {
        List<AlarmInfoPreviewResponse> alarms = alarmUseCase.getAlarms(memberContext.memberId());
        return ApplicationResponse.onSuccess(alarms);
    }

    @CustomErrorCodes(memberErrorCodes = {MEMBER_NOT_FOUND})
    @Operation(summary = "남은 알람 끄기 횟수 조회", description = "회원의 이번 주 남은 알람 끄기 횟수를 조회합니다.")
    @GetMapping("/off-count")
    public ApplicationResponse<AlarmRemainingOffCountResponse> getWeeklyRemainingOffCount(
        @AuthenticationPrincipal MemberContext memberContext
    ) {
        AlarmRemainingOffCountResponse response = alarmUseCase.getWeeklyRemainingOffCount(memberContext.memberId());
        return ApplicationResponse.onSuccess(response);
    }
}
