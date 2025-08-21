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
    ALREADY_DEACTIVATED(HttpStatus.BAD_REQUEST, "ALARM_003", "이미 오늘은 비활성화된 알람입니다."),
    ALARM_OFF_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "ALARM_004", "잔여 알람 끄기 횟수가 부족합니다."),
    REPEAT_DAYS_NOT_CONFIG(HttpStatus.BAD_REQUEST, "ALARM_005", "반복 요일이 설정되지 않았습니다."),
    INVALID_CLIENT_DATE(HttpStatus.BAD_REQUEST, "ALARM_006", "요청의 날짜가 서버 기준 날짜와 일치하지 않습니다."),
    ALARM_DELETE_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "ALARM_007", "지금은 알람을 삭제할 수 없습니다."),
    INVALID_WEEKDAY(HttpStatus.BAD_REQUEST, "ALARM_008", "유효하지 않은 요일 정보입니다."),
    CHECKIN_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "ALARM_009", "지정된 위치 반경 내에 있지 않아 출석할 수 없습니다."),
    NEXT_WEEK_ALARM_DEACTIVATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "ALARM_010", "다음 주에 울릴 알람은 끌 수 없습니다."),
    NOT_ALARM_TIME(HttpStatus.BAD_REQUEST, "ALARM_011", "알람이 울릴 시간이 아닙니다."),

    ALARM_NOT_FOUND(HttpStatus.NOT_FOUND, "ALARM_401", "존재하지 않는 알람입니다."),
    ALARM_OCCURRENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "ALARM_402", "알람 발생 내역이 존재하지 않습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
