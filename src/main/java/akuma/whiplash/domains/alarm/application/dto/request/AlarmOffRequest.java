package akuma.whiplash.domains.alarm.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "알람 끄기 요청 DTO")
public record AlarmOffRequest(

    @Schema(
        description = "현재 클라이언트의 날짜 및 시간 (LocalDateTime)\n" +
            "서버와 클라이언트 간 시간 차이로 인해 알람 대상 날짜를 정확히 계산하기 위함입니다.",
        example = "2025-07-30T08:45:00"
    )
    @NotNull(message = "요청 시간을 입력해주세요.")
    LocalDateTime clientNow
) {

}
