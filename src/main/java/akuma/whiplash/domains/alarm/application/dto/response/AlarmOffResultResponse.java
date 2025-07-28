package akuma.whiplash.domains.alarm.application.dto.response;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record AlarmOffResultResponse(
    LocalDate offTargetDate,        // 끌 알람의 날짜
    String offTargetDayOfWeek,      // 끌 알람의 요일
    LocalDate reactivateDate,       // 토글 다시 켜질 날짜
    String reactivateDayOfWeek,     // 다시 켜질 날짜의 요일
    int remainingOffCount           // 남은 끄기 가능 횟수
) {}