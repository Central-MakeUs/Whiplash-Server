package akuma.whiplash.domains.alarm.domain.service;

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
import akuma.whiplash.domains.alarm.application.dto.response.AlarmPaymentResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import java.util.Set;

public interface AlarmCommandService {

    CreateAlarmResponse createAlarm(AlarmRegisterRequest request, Long memberId, String deviceId);
    AlarmAdSessionCreateResponse createAdSession(Long memberId, Long alarmId, AlarmAdSessionCreateRequest request);
    void removeAlarmByAd(Long memberId, Long alarmId, AlarmDeleteByAdRequest request);
    void removeAlarmByPayment(Long memberId, Long alarmId, AlarmDeleteByPaymentRequest request);
    void removeAlarmByAppStorePayment(Long memberId, String deviceId, Long alarmId, AppStoreAlarmDeletePaymentRequest request);
    void removeAlarmByGooglePlayPayment(Long memberId, String deviceId, Long alarmId, GooglePlayAlarmDeletePaymentRequest request);
    AlarmCheckinResponse checkinAlarm(Long memberId, Long alarmId, AlarmCheckinRequest request);
    AlarmPaymentResponse deactivateByPayment(Long memberId, Long alarmId, AlarmPaymentRequest request);
    AlarmPaymentResponse deactivateByAppStorePayment(Long memberId, String deviceId, Long alarmId, AppStoreAlarmPaymentRequest request);
    AlarmPaymentResponse deactivateByGooglePlayPayment(Long memberId, String deviceId, Long alarmId, GooglePlayAlarmPaymentRequest request);
    void ringAlarm(Long memberId, Long alarmId, String deviceId);
    void markReminderSent(Set<Long> occurrenceIds);
}
