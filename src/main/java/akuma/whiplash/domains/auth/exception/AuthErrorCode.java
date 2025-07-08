package akuma.whiplash.domains.auth.exception;

import akuma.whiplash.global.response.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_101", "사용자 인증에 실패했습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_102", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_103", "만료된 토큰입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_104", "아이디 또는 비밀번호가 일치하지 않습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.UNAUTHORIZED, "AUTH_105", "유효하지 않은 인증번호입니다."),

    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "AUTH_301", "권한이 없습니다."),

    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_401", "존재하지 않는 토큰입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}