package akuma.whiplash.domains.alarm.application.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record GetAlarmsResponse(
    List<AlarmPreviewDto> alarms
) {}
