package akuma.whiplash.domains.ad.exception;

import akuma.whiplash.global.response.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdErrorCode implements BaseErrorCode {

    INVALID_ADMOB_CALLBACK(HttpStatus.BAD_REQUEST, "AD_001", "유효하지 않은 광고 검증 요청입니다."),
    AD_SESSION_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "AD_002", "검증되지 않은 광고 세션입니다."),
    AD_SESSION_EXPIRED(HttpStatus.BAD_REQUEST, "AD_003", "만료된 광고 세션입니다."),
    AD_SESSION_MISMATCH(HttpStatus.FORBIDDEN, "AD_301", "광고 세션 접근 권한이 없습니다."),
    AD_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "AD_401", "광고 세션을 찾을 수 없습니다."),
    AD_SESSION_ALREADY_CONSUMED(HttpStatus.CONFLICT, "AD_901", "이미 사용된 광고 세션입니다.")
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
