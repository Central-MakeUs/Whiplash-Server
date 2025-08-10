package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmInfoPreviewResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmRemainingOffCountResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface AlarmQueryService {
    List<AlarmInfoPreviewResponse> getAlarms(Long memberId);
    List<OccurrencePushInfo> getPreNotificationTargets(LocalDateTime startInclusive, LocalDateTime endInclusive);
    AlarmRemainingOffCountResponse getWeeklyRemainingOffCount(Long memberId);
}
