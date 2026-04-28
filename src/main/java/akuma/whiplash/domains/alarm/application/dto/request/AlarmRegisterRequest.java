package akuma.whiplash.domains.alarm.application.dto.request;

import akuma.whiplash.global.util.date.LocalTime24HourDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "알람 등록 요청 DTO")
public record AlarmRegisterRequest(

    @Schema(description = "장소 정보")
    @NotNull(message = "장소 정보를 입력해주세요.")
    PlaceRequest place,

    @Schema(description = "알람 목적", example = "도서관 정기 출석 알람")
    @NotBlank(message = "알람 목적을 입력해주세요.")
    String alarmPurpose,

    @Schema(description = "알람 시간 (HH:mm)", example = "08:30")
    @NotNull(message = "알람 시간을 입력해주세요.")
    @JsonDeserialize(using = LocalTime24HourDeserializer.class)
    LocalTime alarmTime,

    // TODO: 월~일 요일 검증 필요
    @Schema(description = "반복 요일 리스트 (월~일)", example = "[\"월\", \"수\", \"금\"]")
    @NotNull(message = "반복 요일을 선택해주세요.")
    @Size(min = 1, message = "최소 한 개의 반복 요일을 선택해야 합니다.")
    List<@NotBlank(message = "요일은 비어 있을 수 없습니다.") String> repeatDays,

    @Schema(description = "알람 소리", example = "알람 소리1")
    @NotBlank(message = "알람 소리를 선택해주세요.")
    String soundType
) {

}
