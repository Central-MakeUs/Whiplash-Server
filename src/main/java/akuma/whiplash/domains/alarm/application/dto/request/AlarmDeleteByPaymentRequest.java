package akuma.whiplash.domains.alarm.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "결제로 알람 삭제 요청 DTO")
public record AlarmDeleteByPaymentRequest(

    @Schema(description = "요청 디바이스 UUID", example = "device-uuid")
    @NotBlank(message = "디바이스 ID를 입력해주세요.")
    String deviceId,

    @Schema(description = "인앱 결제 영수증 ID", example = "pay_delete_123")
    @NotBlank(message = "결제 ID를 입력해주세요.")
    String paymentId
) {
}
