package akuma.whiplash.domains.alarm.application.usecase;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmInfoPreviewResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmOffResultResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import akuma.whiplash.domains.alarm.domain.service.AlarmCommandService;
import akuma.whiplash.domains.alarm.domain.service.AlarmQueryService;
import akuma.whiplash.global.annotation.architecture.UseCase;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AlarmUseCase {

    private final AlarmCommandService alarmCommandService;
    private final AlarmQueryService alarmQueryService;

    public void createAlarm(AlarmRegisterRequest request, Long memberId) {
        alarmCommandService.createAlarm(request, memberId);
    }

    public CreateAlarmOccurrenceResponse createAlarmOccurrence(Long memberId, Long alarmId) {
        return alarmCommandService.createAlarmOccurrence(memberId, alarmId);
    }

    public AlarmOffResultResponse alarmOff(Long memberId, Long alarmId, LocalDateTime clientNow) {
        return alarmCommandService.alarmOff(memberId, alarmId, clientNow);
    }

    public void removeAlarm(Long memberId, Long alarmId, String reason) {
        alarmCommandService.removeAlarm(memberId, alarmId, reason);
    }

    public void checkinAlarm(Long memberId, Long alarmId, Long occurrenceId, AlarmCheckinRequest request) {
        alarmCommandService.checkinAlarm(memberId, alarmId, occurrenceId, request);
    }

    public List<AlarmInfoPreviewResponse> getAlarms(Long memberId) {
        return alarmQueryService.getAlarms(memberId);
    }
}
