package akuma.whiplash.domains.alarm.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(description = "알람 목록 개별 항목")
public record AlarmPreviewDto(

    @Schema(description = "알람 ID", example = "10")
    Long alarmId,

    @Schema(description = "알람 목적", example = "출근")
    String alarmPurpose,

    @Schema(description = "알람 시간 (HH:mm)", example = "08:00")
    String alarmTime,

    @Schema(description = "반복 요일 단축형", example = "[\"월\",\"화\",\"수\",\"목\",\"금\"]")
    List<String> repeatDays,

    @Schema(description = "목표 장소 주소", example = "서울특별시 강남구 테헤란로 123")
    String address,

    @Schema(description = "알람 상태 (alarm.status 기준)", example = "활성화")
    String status,

    @Schema(description = "도착 인증 버튼 활성화 여부 (알람 3시간 전부터 true)", example = "true")
    Boolean arrivalCheckEnabled,

    @Schema(description = "다음 알람 발생 정보")
    OccurrenceInfo nextOccurrence,

    @Schema(description = "다다음 알람 발생 정보")
    OccurrenceInfo nextNextOccurrence
) {
    @Builder
    public record OccurrenceInfo(
        @Schema(description = "알람 발생 내역 ID (배치 생성 전이면 null)", example = "1")
        Long occurrenceId,

        @Schema(description = "예정 날짜", example = "2026-04-22")
        LocalDate scheduledDate,

        @Schema(description = "요일 단축형", example = "월")
        String dayOfWeek
    ) {}
}
