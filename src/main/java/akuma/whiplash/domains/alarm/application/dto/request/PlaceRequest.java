package akuma.whiplash.domains.alarm.application.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "장소 정보 요청 DTO")
public record PlaceRequest(
    @Schema(description = "장소 주소 캐시. 일반 지도 핀은 /reverse-geocode 주소를 전달할 수 있으며 생략 시 서버가 목록 조회 때 좌표로 갱신합니다.", example = "대한민국 서울특별시 중구 세종대로 110", nullable = true)
    String address,

    @Schema(description = "위도", example = "37.564213")
    @NotNull(message = "위도를 입력해주세요.")
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90 이하이어야 합니다.")
    Double latitude,

    @Schema(description = "경도", example = "127.001698")
    @NotNull(message = "경도를 입력해주세요.")
    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 -180 이하이어야 합니다.")
    Double longitude,

    @Schema(description = "Google Places 장소 식별자. 일반 지도 핀은 생략합니다.", example = "ChIJz2v...", nullable = true)
    @Size(max = 255, message = "Google Place ID는 255자 이하여야 합니다.")
    String googlePlaceId,

    @Schema(description = "장소 자동완성 세션 토큰", nullable = true)
    @Size(max = 64, message = "장소 세션 토큰은 64자 이하여야 합니다.")
    String sessionToken
) {

    @JsonIgnore
    @AssertTrue(message = "Google 장소에는 공백이 아닌 Place ID가 필요하며 일반 지도 핀에는 세션 토큰을 보내지 않아야 합니다.")
    public boolean isValidPlaceSelection() {
        if (googlePlaceId == null) {
            return (address == null || !address.isBlank()) && sessionToken == null;
        }
        return !googlePlaceId.isBlank() && (address == null || !address.isBlank());
    }
}
