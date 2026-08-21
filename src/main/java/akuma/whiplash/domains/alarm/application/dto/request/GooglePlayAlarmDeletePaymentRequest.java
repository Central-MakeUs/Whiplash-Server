package akuma.whiplash.domains.alarm.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Google Play 결제로 알람 삭제 요청 DTO")
public record GooglePlayAlarmDeletePaymentRequest(

    @Schema(description = "Google Play 구매 토큰", example = "purchase-token")
    @NotBlank(message = "Google Play 구매 토큰을 입력해주세요.")
    String purchaseToken,

    @Schema(description = "Google Play 상품 ID", example = "ALARM_DELETE")
    @NotBlank(message = "상품 ID를 입력해주세요.")
    String productId
) {
}
