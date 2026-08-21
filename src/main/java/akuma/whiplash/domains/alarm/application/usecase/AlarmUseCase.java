package akuma.whiplash.domains.alarm.application.usecase;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmAdSessionCreateRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AppStoreAlarmDeletePaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AppStoreAlarmPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.GooglePlayAlarmDeletePaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.GooglePlayAlarmPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmDeleteByAdRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmDeleteByPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmAdSessionCreateResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmCheckinResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmDeleteMethodResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmPaymentResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmSyncResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import akuma.whiplash.domains.alarm.application.dto.response.GetAlarmsResponse;
import akuma.whiplash.domains.alarm.domain.service.AlarmCommandService;
import akuma.whiplash.domains.alarm.domain.service.AlarmQueryService;
import akuma.whiplash.domains.payment.domain.command.StorePaymentProof;
import akuma.whiplash.global.annotation.architecture.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AlarmUseCase {

    private final AlarmCommandService alarmCommandService;
    private final AlarmQueryService alarmQueryService;

    public CreateAlarmResponse createAlarm(AlarmRegisterRequest request, Long memberId, String deviceId) {
        return alarmCommandService.createAlarm(request, memberId, deviceId);
    }

    public void removeAlarmByPayment(Long memberId, Long alarmId, AlarmDeleteByPaymentRequest request) {
        alarmCommandService.removeAlarmByPayment(memberId, alarmId, request);
    }

    public void removeAlarmByAppStorePayment(Long memberId, String deviceId, Long alarmId, AppStoreAlarmDeletePaymentRequest request) {
        alarmCommandService.removeAlarmByAppStorePayment(memberId, deviceId, alarmId,
            new StorePaymentProof(request.transactionId(), request.productId()));
    }

    public void removeAlarmByGooglePlayPayment(Long memberId, String deviceId, Long alarmId, GooglePlayAlarmDeletePaymentRequest request) {
        alarmCommandService.removeAlarmByGooglePlayPayment(memberId, deviceId, alarmId,
            new StorePaymentProof(request.purchaseToken(), request.productId()));
    }

    public AlarmAdSessionCreateResponse createAdSession(Long memberId, Long alarmId, AlarmAdSessionCreateRequest request) {
        return alarmCommandService.createAdSession(memberId, alarmId, request);
    }

    public void removeAlarmByAd(Long memberId, Long alarmId, AlarmDeleteByAdRequest request) {
        alarmCommandService.removeAlarmByAd(memberId, alarmId, request);
    }

    public AlarmCheckinResponse checkinAlarm(Long memberId, Long alarmId, AlarmCheckinRequest request) {
        return alarmCommandService.checkinAlarm(memberId, alarmId, request);
    }

    public AlarmPaymentResponse deactivateByPayment(Long memberId, Long alarmId, AlarmPaymentRequest request) {
        return alarmCommandService.deactivateByPayment(memberId, alarmId, request);
    }

    public AlarmPaymentResponse deactivateByAppStorePayment(Long memberId, String deviceId, Long alarmId, AppStoreAlarmPaymentRequest request) {
        return alarmCommandService.deactivateByAppStorePayment(memberId, deviceId, alarmId, request.occurrenceId(),
            new StorePaymentProof(request.transactionId(), request.productId()));
    }

    public AlarmPaymentResponse deactivateByGooglePlayPayment(Long memberId, String deviceId, Long alarmId, GooglePlayAlarmPaymentRequest request) {
        return alarmCommandService.deactivateByGooglePlayPayment(memberId, deviceId, alarmId, request.occurrenceId(),
            new StorePaymentProof(request.purchaseToken(), request.productId()));
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

    public AlarmDeleteMethodResponse getAlarmDeleteMethod(Long memberId, Long alarmId) {
        return alarmQueryService.getAlarmDeleteMethod(memberId, alarmId);
    }
}
