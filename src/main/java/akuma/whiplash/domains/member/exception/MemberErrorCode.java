package akuma.whiplash.domains.member.exception;

import akuma.whiplash.global.response.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_401", "존재하지 않는 회원입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
