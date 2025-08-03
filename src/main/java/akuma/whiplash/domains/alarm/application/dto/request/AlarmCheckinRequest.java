package akuma.whiplash.domains.alarm.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Schema(description = "알람 도착 인증 요청 DTO")
public record AlarmCheckinRequest(

    @Schema(description = "현재 사용자 위치의 위도", example = "37.564213")
    @NotNull(message = "현재 사용자 위치의 위도를 입력해주세요.")
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90 이하이어야 합니다.")
    double latitude,

    @Schema(description = "현재 사용자 위치의 경도", example = "127.001698")
    @NotNull(message = "현재 사용자 위치의 경도를 입력해주세요.")
    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 180 이하이어야 합니다.")
    double longitude
) {

}
