package akuma.whiplash.domains.alarm.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "장소 정보 요청 DTO")
public record PlaceRequest(
    @Schema(description = "장소 주소", example = "서울시 중구 퇴계로 24")
    @NotBlank(message = "장소 주소를 입력해주세요.")
    String address,

    @Schema(description = "위도", example = "37.564213")
    @NotNull(message = "위도를 입력해주세요.")
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90 이하이어야 합니다.")
    double latitude,

    @Schema(description = "경도", example = "127.001698")
    @NotNull(message = "경도를 입력해주세요.")
    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 -180 이하이어야 합니다.")
    double longitude
) {
}
