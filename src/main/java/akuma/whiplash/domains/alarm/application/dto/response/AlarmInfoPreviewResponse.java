package akuma.whiplash.domains.alarm.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(description = "알람 목록 조회에 들어가는 개별 알람 정보")
public record AlarmInfoPreviewResponse(

    @Schema(description = "알람 ID", example = "9")
    Long alarmId,

    @Schema(description = "알람 목적 (알람 이름)", example = "독서실 가는 알람")
    String alarmPurpose,

    @Schema(
        description = "반복 요일 목록 (한글 요일, 예: 월, 수, 금)",
        example = "[\"월\", \"목\", \"토\"]"
    )
    List<String> repeatsDays,

    @Schema(description = "알람 울릴 시간 (HH:mm 형식)", example = "19:30")
    String time,

    @Schema(description = "알람 장소 주소", example = "서울시 중구 퇴계로 24")
    String address,

    @Schema(description = "알람 장소 위도", example = "37.564213")
    Double latitude,

    @Schema(description = "알람 장소 경도", example = "127.001698")
    Double longitude,

    @Schema(description = "현재 알람이 ON 상태인지 여부", example = "true")
    Boolean isToggleOn,

    @Schema(description = "이번에 울릴 알람 날짜 (알람 꺼짐 여부와 무관하게 다음 울릴 텀)", example = "2025-08-04")
    LocalDate firstUpcomingDay,

    @Schema(description = "이번에 울릴 알람 요일", example = "월요일")
    String firstUpcomingDayOfWeek,

    @Schema(description = "그 다음 텀의 알람 날짜", example = "2025-08-07")
    LocalDate secondUpcomingDay,

    @Schema(description = "그 다음 텀의 알람 요일", example = "목요일")
    String secondUpcomingDayOfWeek

) {}
