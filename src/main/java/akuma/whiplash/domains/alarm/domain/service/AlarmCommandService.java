package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.request.RegisterAlarmRequest;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;

public interface AlarmCommandService {

    void createAlarm(RegisterAlarmRequest request, Long memberId);
    CreateAlarmOccurrenceResponse createAlarmOccurrence(Long alarmId);
}
