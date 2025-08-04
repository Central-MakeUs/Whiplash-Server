package akuma.whiplash.domains.auth.exception;

import akuma.whiplash.global.response.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    UNSUPPORTED_SOCIAL_TYPE(HttpStatus.BAD_REQUEST, "AUTH_001", "지원하지 않는 소셜 로그인 타입입니다."),

    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_101", "사용자 인증에 실패했습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_102", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_103", "만료된 토큰입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_104", "아이디 또는 비밀번호가 일치하지 않습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.UNAUTHORIZED, "AUTH_105", "유효하지 않은 인증번호입니다."),

    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "AUTH_301", "권한이 없습니다."),
    IS_SLEEPER_ACCOUNT(HttpStatus.FORBIDDEN, "AUTH_302", "휴면 처리된 계정입니다. 관리자에게 문의해주세요."),

    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_401", "존재하지 않는 토큰입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}