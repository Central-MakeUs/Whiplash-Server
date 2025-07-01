package akuma.whiplash.global.response.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessCode {

    SUCCESS(HttpStatus.OK, "SUCCESS", "성공")
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}