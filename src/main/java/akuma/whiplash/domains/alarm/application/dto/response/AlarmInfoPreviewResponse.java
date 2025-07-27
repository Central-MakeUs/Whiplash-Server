package akuma.whiplash.domains.alarm.application.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record AlarmInfoPreviewResponse(
    Long alarmId,
    String alarmPurpose,
    List<String> repeatsDays,
    String time,
    String address,
    Double latitude,
    Double longitude,
    Boolean isToggleOn
) {

}
