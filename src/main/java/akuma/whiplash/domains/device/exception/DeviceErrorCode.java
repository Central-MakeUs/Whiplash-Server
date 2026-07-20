package akuma.whiplash.domains.device.exception;

import akuma.whiplash.global.response.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DeviceErrorCode implements BaseErrorCode {

    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "DEVICE_x001", "기기를 찾을 수 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
