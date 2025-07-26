package akuma.whiplash.domains.alarm.exception;

import akuma.whiplash.global.response.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AlarmErrorCode implements BaseErrorCode {

    TODAY_IS_NOT_ALARM_DAY(HttpStatus.BAD_REQUEST, "ALARM_001", "오늘은 알람이 울리는 날이 아닙니다."),
    ALREADY_OCCURRED_EXISTS(HttpStatus.BAD_REQUEST, "ALARM_002", "알람 발생 내역이 이미 생성돼 있습니다."),

    ALARM_NOT_FOUND(HttpStatus.NOT_FOUND, "ALARM_401", "존재하지 않는 알람입니다.")
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
