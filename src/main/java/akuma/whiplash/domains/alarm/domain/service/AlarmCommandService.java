package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmOffResultResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import java.time.LocalDateTime;

public interface AlarmCommandService {

    void createAlarm(AlarmRegisterRequest request, Long memberId);
    CreateAlarmOccurrenceResponse createAlarmOccurrence(Long memberId, Long alarmId);
    AlarmOffResultResponse alarmOff(Long memberId, Long alarmId, LocalDateTime clientNow);
    void removeAlarm(Long memberId, Long alarmId, String reason);
    void checkinAlarm(Long memberId, Long alarmId, AlarmCheckinRequest request);
}
