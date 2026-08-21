package akuma.whiplash.domains.alarm.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "App Store 결제로 알람 삭제 요청 DTO")
public record AppStoreAlarmDeletePaymentRequest(

    @Schema(description = "App Store 거래 ID", example = "2000000123456789")
    @NotBlank(message = "App Store 거래 ID를 입력해주세요.")
    String transactionId,

    @Schema(description = "App Store 상품 ID", example = "ALARM_DELETE")
    @NotBlank(message = "상품 ID를 입력해주세요.")
    String productId
) {
}
