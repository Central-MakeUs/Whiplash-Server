package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmAdActionRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmOffAdSessionCreateRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmAdSessionCreateResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmCheckinResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmDeactivationResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import java.util.Set;

public interface AlarmCommandService {

    CreateAlarmResponse createAlarm(AlarmRegisterRequest request, Long memberId, String deviceId);
    AlarmAdSessionCreateResponse createOffAdSession(Long memberId, String deviceId, Long alarmId, AlarmOffAdSessionCreateRequest request);
    AlarmDeactivationResponse deactivateByAd(Long memberId, String deviceId, Long alarmId, AlarmAdActionRequest request);
    AlarmAdSessionCreateResponse createDeleteAdSession(Long memberId, String deviceId, Long alarmId);
    void removeAlarmByAd(Long memberId, String deviceId, Long alarmId, AlarmAdActionRequest request);
    AlarmCheckinResponse checkinAlarm(Long memberId, Long alarmId, AlarmCheckinRequest request);
    void ringAlarm(Long memberId, Long alarmId, String deviceId);
    void markReminderSent(Set<Long> occurrenceIds);
}
