package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo;
import akuma.whiplash.domains.alarm.application.dto.etc.RingingPushInfo;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmSyncResponse;
import akuma.whiplash.domains.alarm.application.dto.response.GetAlarmsResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface AlarmQueryService {
    GetAlarmsResponse getAlarms(Long memberId, String deviceId);
    AlarmSyncResponse getSyncAlarms(Long memberId, String deviceId);
    List<OccurrencePushInfo> getPreNotificationTargets(LocalDateTime startInclusive, LocalDateTime endInclusive);
    List<RingingPushInfo> getRingingNotificationTargets();
}
