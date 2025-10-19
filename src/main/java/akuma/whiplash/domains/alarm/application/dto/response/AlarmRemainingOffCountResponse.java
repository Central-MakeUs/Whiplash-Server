package akuma.whiplash.domains.alarm.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "남은 알람 끄기 횟수 응답")
public record AlarmRemainingOffCountResponse(
    @Schema(
        description = "회원의 남은 알람 끄기 횟수(회원당 매주 2회 부여, 매주 월요일 초기화)",
        example = "1",
        minimum = "0"
    )
    int remainingOffCount
) {}