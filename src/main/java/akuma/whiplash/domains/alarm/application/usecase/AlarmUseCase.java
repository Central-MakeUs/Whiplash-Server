package akuma.whiplash.domains.alarm.application.usecase;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmDeleteByAdRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmDeleteByPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmCheckinResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmDeleteByAdResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmDeleteByPaymentResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmPaymentResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmSyncResponse;
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

    public AlarmDeleteByPaymentResponse removeAlarmByPayment(Long memberId, Long alarmId, AlarmDeleteByPaymentRequest request) {
        return alarmCommandService.removeAlarmByPayment(memberId, alarmId, request);
    }

    public AlarmDeleteByAdResponse removeAlarmByAd(Long memberId, Long alarmId, AlarmDeleteByAdRequest request) {
        return alarmCommandService.removeAlarmByAd(memberId, alarmId, request);
    }

    public AlarmCheckinResponse checkinAlarm(Long memberId, Long alarmId, AlarmCheckinRequest request) {
        return alarmCommandService.checkinAlarm(memberId, alarmId, request);
    }

    public AlarmPaymentResponse deactivateByPayment(Long memberId, Long alarmId, AlarmPaymentRequest request) {
        return alarmCommandService.deactivateByPayment(memberId, alarmId, request);
    }

    public void ringAlarm(Long memberId, Long alarmId) {
        alarmCommandService.ringAlarm(memberId, alarmId);
    }

    public GetAlarmsResponse getAlarms(Long memberId) {
        return alarmQueryService.getAlarms(memberId);
    }

    public AlarmSyncResponse getSyncAlarms(Long memberId) {
        return alarmQueryService.getSyncAlarms(memberId);
    }
}
