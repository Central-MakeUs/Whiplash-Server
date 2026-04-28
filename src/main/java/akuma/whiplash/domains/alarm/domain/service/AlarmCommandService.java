package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmCheckinResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import java.util.Set;

public interface AlarmCommandService {

    CreateAlarmResponse createAlarm(AlarmRegisterRequest request, Long memberId);
    CreateAlarmOccurrenceResponse createAlarmOccurrence(Long memberId, Long alarmId);
    void removeAlarm(Long memberId, Long alarmId, String reason);
    AlarmCheckinResponse checkinAlarm(Long memberId, Long alarmId, AlarmCheckinRequest request);
    void ringAlarm(Long memberId, Long alarmId);
    void markReminderSent(Set<Long> occurrenceIds);
}
