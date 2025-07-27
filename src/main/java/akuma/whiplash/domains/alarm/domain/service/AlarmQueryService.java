package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.response.AlarmInfoPreviewResponse;
import java.util.List;

public interface AlarmQueryService {
    List<AlarmInfoPreviewResponse> getAlarms(Long memberId);
}
