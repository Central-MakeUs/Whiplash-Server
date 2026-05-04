package akuma.whiplash.domains.payment.exception;

import akuma.whiplash.global.response.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements BaseErrorCode {

    PAYMENT_NOT_YET_AVAILABLE(HttpStatus.BAD_REQUEST, "PAYMENT_001", "아직 결제로 알람을 끌 수 있는 시간이 아닙니다."),
    PAYMENT_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT_002", "결제 검증에 실패했습니다."),
    DUPLICATE_PAYMENT(HttpStatus.CONFLICT, "PAYMENT_901", "이미 처리된 결제입니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_401", "존재하지 않는 결제입니다.");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
