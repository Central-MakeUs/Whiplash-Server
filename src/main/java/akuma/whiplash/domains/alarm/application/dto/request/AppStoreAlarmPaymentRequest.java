package akuma.whiplash.domains.alarm.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "App Store 결제로 알람 끄기 요청 DTO")
public record AppStoreAlarmPaymentRequest(

    @Schema(description = "알람 발생 회차 ID", example = "501")
    @NotNull(message = "알람 발생 회차 ID를 입력해주세요.")
    Long occurrenceId,

    @Schema(description = "App Store 거래 ID", example = "2000000123456789")
    @NotBlank(message = "App Store 거래 ID를 입력해주세요.")
    String transactionId,

    @Schema(description = "App Store 상품 ID", example = "ALARM_OFF")
    @NotBlank(message = "상품 ID를 입력해주세요.")
    String productId
) {
}
