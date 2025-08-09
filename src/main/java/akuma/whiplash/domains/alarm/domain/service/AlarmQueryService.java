package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmInfoPreviewResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AlarmQueryService {
    List<AlarmInfoPreviewResponse> getAlarms(Long memberId);
    List<OccurrencePushInfo> findPushTargetsByTimeRange(LocalDate date, LocalTime start, LocalTime end);
}
