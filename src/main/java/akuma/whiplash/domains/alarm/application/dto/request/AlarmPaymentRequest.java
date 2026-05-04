package akuma.whiplash.domains.alarm.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "결제로 알람 끄기 요청 DTO")
public record AlarmPaymentRequest(

    @Schema(description = "알람 발생 회차 ID", example = "501")
    @NotNull(message = "알람 발생 회차 ID를 입력해주세요.")
    Long occurrenceId,

    @Schema(description = "요청 디바이스 UUID", example = "device-uuid")
    @NotBlank(message = "디바이스 ID를 입력해주세요.")
    String deviceId,

    @Schema(description = "인앱 결제 영수증 ID", example = "pay_123")
    @NotBlank(message = "결제 ID를 입력해주세요.")
    String paymentId
) {
}
