package akuma.whiplash.domains.alarm.presentation;

import akuma.whiplash.domains.alarm.application.dto.request.RegisterAlarmRequest;
import akuma.whiplash.domains.alarm.application.usecase.AlarmUseCase;
import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.global.response.ApplicationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping
    public ApplicationResponse<Void> registerAlarm(@AuthenticationPrincipal MemberContext memberContext, @RequestBody @Valid RegisterAlarmRequest request) {
        alarmUseCase.registerAlarm(request, memberContext.memberId());
        return ApplicationResponse.onSuccess();
    }
}