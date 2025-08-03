package akuma.whiplash.domains.alarm.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "알람 삭제 요청 DTO")
public record AlarmRemoveRequest(

    @Schema(description = "알람 삭제 이유", example = "알람이 너무 자주 울려서요.")
    @NotBlank(message = "알람 삭제 이유를 입력해주세요")
    String reason
) {
}