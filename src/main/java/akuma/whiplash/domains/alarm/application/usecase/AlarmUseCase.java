package akuma.whiplash.domains.alarm.application.usecase;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmAdActionRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmOffAdSessionCreateRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmAdSessionCreateResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmCheckinResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmDeactivationResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmSyncResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import akuma.whiplash.domains.alarm.application.dto.response.GetAlarmsResponse;
import akuma.whiplash.domains.alarm.application.dto.response.LocationPreparationResponse;
import akuma.whiplash.domains.alarm.application.service.AlarmLocationPreparationService;
import akuma.whiplash.domains.alarm.domain.service.AlarmCommandService;
import akuma.whiplash.domains.alarm.domain.service.AlarmQueryService;
import akuma.whiplash.global.annotation.architecture.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AlarmUseCase {

    private final AlarmCommandService alarmCommandService;
    private final AlarmQueryService alarmQueryService;
    private final AlarmLocationPreparationService alarmLocationPreparationService;

    public CreateAlarmResponse createAlarm(AlarmRegisterRequest request, Long memberId, String deviceId) {
        return alarmCommandService.createAlarm(request, memberId, deviceId);
    }

    public AlarmAdSessionCreateResponse createOffAdSession(Long memberId, String deviceId, Long alarmId, AlarmOffAdSessionCreateRequest request) {
        return alarmCommandService.createOffAdSession(memberId, deviceId, alarmId, request);
    }

    public AlarmDeactivationResponse deactivateByAd(Long memberId, String deviceId, Long alarmId, AlarmAdActionRequest request) {
        return alarmCommandService.deactivateByAd(memberId, deviceId, alarmId, request);
    }

    public AlarmAdSessionCreateResponse createDeleteAdSession(Long memberId, String deviceId, Long alarmId) {
        return alarmCommandService.createDeleteAdSession(memberId, deviceId, alarmId);
    }

    public void removeAlarmByAd(Long memberId, String deviceId, Long alarmId, AlarmAdActionRequest request) {
        alarmCommandService.removeAlarmByAd(memberId, deviceId, alarmId, request);
    }

    public AlarmCheckinResponse checkinAlarm(Long memberId, Long alarmId, AlarmCheckinRequest request) {
        return alarmCommandService.checkinAlarm(memberId, alarmId, request);
    }

    public void ringAlarm(Long memberId, Long alarmId, String deviceId) {
        alarmCommandService.ringAlarm(memberId, alarmId, deviceId);
    }

    public GetAlarmsResponse getAlarms(Long memberId, String deviceId) {
        return alarmQueryService.getAlarms(memberId, deviceId);
    }

    public AlarmSyncResponse getSyncAlarms(Long memberId, String deviceId) {
        return alarmQueryService.getSyncAlarms(memberId, deviceId);
    }

    public LocationPreparationResponse getLocationPreparation(Long memberId, Long alarmId, Long occurrenceId) {
        return alarmLocationPreparationService.getLocationPreparation(memberId, alarmId, occurrenceId);
    }
}
