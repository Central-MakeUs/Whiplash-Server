package akuma.whiplash.domains.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "토큰 재발급 DTO")
public record ReissueRequest(

    @Schema(name = "디바이스 ID")
    @NotBlank(message = "디바이스 ID를 입력해주세요.")
    String deviceId
) {

}
