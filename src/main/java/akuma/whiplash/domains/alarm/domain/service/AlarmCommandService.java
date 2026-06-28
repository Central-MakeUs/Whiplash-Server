package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmDeleteByAdRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmDeleteByPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmPaymentRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmCheckinResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmPaymentResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import java.util.Set;

public interface AlarmCommandService {

    CreateAlarmResponse createAlarm(AlarmRegisterRequest request, Long memberId, String deviceId);
    CreateAlarmOccurrenceResponse createAlarmOccurrence(Long memberId, Long alarmId);
    void removeAlarmByAd(Long memberId, Long alarmId, AlarmDeleteByAdRequest request);
    void removeAlarmByPayment(Long memberId, Long alarmId, AlarmDeleteByPaymentRequest request);
    AlarmCheckinResponse checkinAlarm(Long memberId, Long alarmId, AlarmCheckinRequest request);
    AlarmPaymentResponse deactivateByPayment(Long memberId, Long alarmId, AlarmPaymentRequest request);
    void ringAlarm(Long memberId, Long alarmId, String deviceId);
    void markReminderSent(Set<Long> occurrenceIds);
}
