package akuma.whiplash.domains.payment.exception;

import akuma.whiplash.global.response.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements BaseErrorCode {

    DUPLICATE_PAYMENT(HttpStatus.CONFLICT, "PAYMENT_901", "이미 처리된 결제입니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_401", "존재하지 않는 결제입니다.");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
