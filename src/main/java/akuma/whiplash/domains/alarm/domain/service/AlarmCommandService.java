package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.request.RegisterAlarmRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmOffResultResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import java.time.LocalDateTime;

public interface AlarmCommandService {

    void createAlarm(RegisterAlarmRequest request, Long memberId);
    CreateAlarmOccurrenceResponse createAlarmOccurrence(Long memberId, Long alarmId);
    AlarmOffResultResponse alarmOff(Long memberId, Long alarmId, LocalDateTime clientNow);
}
