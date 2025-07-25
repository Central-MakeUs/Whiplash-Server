package akuma.whiplash.domains.alarm.application.usecase;

import akuma.whiplash.domains.alarm.application.dto.request.RegisterAlarmRequest;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import akuma.whiplash.domains.alarm.domain.service.AlarmCommandService;
import akuma.whiplash.global.annotation.architecture.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AlarmUseCase {

    private final AlarmCommandService alarmCommandService;

    public void createAlarm(RegisterAlarmRequest request, Long memberId) {
        alarmCommandService.createAlarm(request, memberId);
    }

    public CreateAlarmOccurrenceResponse createAlarmOccurrence(Long alarmId) {
        return alarmCommandService.createAlarmOccurrence(alarmId);
    }
}
