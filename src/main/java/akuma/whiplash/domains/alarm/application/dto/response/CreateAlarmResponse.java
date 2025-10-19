package akuma.whiplash.domains.alarm.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "알람 등록 응답 DTO")
public record CreateAlarmResponse(

    @Schema(description = "알람 PK", example = "1")
    Long alarmId
) {

}
