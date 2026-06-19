package akuma.whiplash.domains.place.exception;

import akuma.whiplash.global.response.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements BaseErrorCode {

    INVALID_COORDINATE(HttpStatus.BAD_REQUEST, "PLACE_001", "올바르지 않은 좌표입니다."),
    UNSUPPORTED_LANGUAGE(HttpStatus.BAD_REQUEST, "PLACE_002", "지원하지 않는 언어입니다."),
    UNSUPPORTED_REGION(HttpStatus.BAD_REQUEST, "PLACE_003", "지원하지 않는 지역입니다."),
    PROVIDER_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "PLACE_101", "장소 제공자 인증에 실패했습니다."),
    PROVIDER_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "PLACE_301", "장소 제공자 요청 권한이 없습니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE_401", "좌표에 해당하는 장소를 찾을 수 없습니다."),
    PROVIDER_QUOTA_EXCEEDED(HttpStatus.CONFLICT, "PLACE_901", "장소 제공자 사용량 제한을 초과했습니다."),
    PROVIDER_TIMEOUT(HttpStatus.CONFLICT, "PLACE_902", "장소 제공자 응답 시간이 초과되었습니다."),
    PROVIDER_ERROR(HttpStatus.CONFLICT, "PLACE_903", "장소 제공자 요청 처리에 실패했습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
