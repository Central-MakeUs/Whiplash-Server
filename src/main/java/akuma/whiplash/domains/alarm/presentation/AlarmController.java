package akuma.whiplash.domains.alarm.presentation;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.*;
import static akuma.whiplash.domains.member.exception.MemberErrorCode.MEMBER_NOT_FOUND;

import akuma.whiplash.domains.alarm.application.dto.request.RegisterAlarmRequest;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import akuma.whiplash.domains.alarm.application.usecase.AlarmUseCase;
import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.global.annotation.swagger.CustomErrorCodes;
import akuma.whiplash.global.response.ApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ApplicationResponse<Void> createAlarm(@AuthenticationPrincipal MemberContext memberContext, @RequestBody @Valid RegisterAlarmRequest request) {
        alarmUseCase.createAlarm(request, memberContext.memberId());
        return ApplicationResponse.onSuccess();
    }


    @CustomErrorCodes(alarmErrorCodes = {ALARM_NOT_FOUND, TODAY_IS_NOT_ALARM_DAY, ALREADY_OCCURRED_EXISTS})
    @Operation(summary = "알람 발생 내역 생성", description = "오늘 울려야할 알람이 처음 울렸을 때 호출하는 API입니다. 알람당 하루에 발생 내역은 1개만 생성할 수 있습니다.")
    @PostMapping("/{alarmId}/occurrences")
    public ApplicationResponse<CreateAlarmOccurrenceResponse> createAlarmOccurrence(@AuthenticationPrincipal MemberContext memberContext, @PathVariable Long alarmId) {
        CreateAlarmOccurrenceResponse response = alarmUseCase.createAlarmOccurrence(alarmId);
        return ApplicationResponse.onSuccess(response);
    }
}