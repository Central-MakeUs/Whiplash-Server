package akuma.whiplash.domains.alarm.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;

@Builder
@Schema(description = "알람 끄기 결과 정보")
public record AlarmOffResultResponse(

    @Schema(description = "꺼진 알람 날짜", example = "2025-08-07")
    LocalDate offTargetDate,        // 끌 알람의 날짜

    @Schema(description = "꺼진 알람 요일", example = "목요일")
    String offTargetDayOfWeek,      // 끌 알람의 요일

    @Schema(description = "다음에 울릴 알람의 날짜", example = "2025-08-10")
    LocalDate reactivateDate,       // 토글 다시 켜질 날짜

    @Schema(description = "다음에 울릴 알람의 요일", example = "일요일")
    String reactivateDayOfWeek,     // 다시 켜질 날짜의 요일

    @Schema(description = "남은 알람 끄기 가능 횟수", example = "1")
    int remainingOffCount           // 남은 끄기 가능 횟수
) {}