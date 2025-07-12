package akuma.whiplash.domains.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(

    @Schema(description = "디바이스 ID")
    @NotBlank(message = "디바이스 ID를 입력해주세요.")
    String deviceId
) {

}
