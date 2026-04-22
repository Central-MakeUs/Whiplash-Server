package akuma.whiplash.domains.alarm.application.usecase;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import akuma.whiplash.domains.alarm.application.dto.response.GetAlarmsResponse;
import akuma.whiplash.domains.alarm.domain.service.AlarmCommandService;
import akuma.whiplash.domains.alarm.domain.service.AlarmQueryService;
import akuma.whiplash.global.annotation.architecture.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AlarmUseCase {

    private final AlarmCommandService alarmCommandService;
    private final AlarmQueryService alarmQueryService;

    public CreateAlarmResponse createAlarm(AlarmRegisterRequest request, Long memberId) {
        return alarmCommandService.createAlarm(request, memberId);
    }

    public CreateAlarmOccurrenceResponse createAlarmOccurrence(Long memberId, Long alarmId) {
        return alarmCommandService.createAlarmOccurrence(memberId, alarmId);
    }

    public void removeAlarm(Long memberId, Long alarmId, String reason) {
        alarmCommandService.removeAlarm(memberId, alarmId, reason);
    }

    public void checkinAlarm(Long memberId, Long alarmId, AlarmCheckinRequest request) {
        alarmCommandService.checkinAlarm(memberId, alarmId, request);
    }

    public void ringAlarm(Long memberId, Long alarmId) {
        alarmCommandService.ringAlarm(memberId, alarmId);
    }

    public GetAlarmsResponse getAlarms(Long memberId) {
        return alarmQueryService.getAlarms(memberId);
    }
}
