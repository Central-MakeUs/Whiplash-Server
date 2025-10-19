package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmOffResultResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import java.time.LocalDateTime;
import java.util.Set;

public interface AlarmCommandService {

    CreateAlarmResponse createAlarm(AlarmRegisterRequest request, Long memberId);
    CreateAlarmOccurrenceResponse createAlarmOccurrence(Long memberId, Long alarmId);
    AlarmOffResultResponse alarmOff(Long memberId, Long alarmId, LocalDateTime clientNow);
    void removeAlarm(Long memberId, Long alarmId, String reason);
    void checkinAlarm(Long memberId, Long alarmId, AlarmCheckinRequest request);
    void ringAlarm(Long memberId, Long alarmId);
    void markReminderSent(Set<Long> occurrenceIds);
}
