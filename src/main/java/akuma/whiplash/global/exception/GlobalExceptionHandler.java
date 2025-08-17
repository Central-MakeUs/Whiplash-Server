package akuma.whiplash.global.exception;

import akuma.whiplash.global.response.ApplicationResponse;
import akuma.whiplash.global.response.code.BaseErrorCode;
import akuma.whiplash.global.response.code.CommonErrorCode;
import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice(annotations = {RestController.class})
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException e,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        e.getBindingResult()
            .getFieldErrors()
            .forEach(fieldError -> {
                String fieldName = fieldError.getField();
                String errorMessage = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
                errors.merge(fieldName, errorMessage,
                    (existingErrorMessage, newErrorMessage) -> existingErrorMessage + ", " + newErrorMessage);
            });

        sendErrorToSentry(
            e,
            request.getDescription(false), // ex: "uri=/api/alarms/100/checkin"
            request.getParameterMap().toString(), // 쿼리 스트링 (없으면 빈 Map)
            e.getBody().getTitle()
        );

        return handleExceptionInternalArgs(
            e,
            request,
            errors
        );
    }

    @ExceptionHandler
    public ResponseEntity<Object> validation(ConstraintViolationException e, WebRequest request) {
        String errorMessage = e.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("ConstraintViolationException Error"));

        sendErrorToSentry(
            e,
            request.getDescription(false),
            e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + "=" + v.getInvalidValue())
                .collect(Collectors.joining(", ")),
            errorMessage
            );

        return handleExceptionInternalConstraint(e, CommonErrorCode.valueOf(errorMessage), request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        log.error("HttpMessageNotReadableException: {}", ex.getMessage());
        ApplicationResponse<Void> response = ApplicationResponse.onFailure(
            CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCustomCode(),
            CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getMessage()
        );

        sendErrorToSentry(
            ex,
            request.getDescription(false),
            request.getParameterMap().toString(),
            CommonErrorCode.METHOD_ARGUMENT_NOT_VALID.getCustomCode()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<Object> exception(Exception e, WebRequest request) {
        log.error("Unexpected error: ", e);

        sendErrorToSentry(
            e,
            request.getDescription(false),
            request.getParameterMap().toString(),
            CommonErrorCode.INTERNAL_SERVER_ERROR.getCustomCode()
        );

        return handleExceptionInternalFalse(
            e,
            CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus(),
            request,
            e.getMessage()
        );
    }

    @ExceptionHandler(value = ApplicationException.class)
    public ResponseEntity<Object> onThrowException(ApplicationException ex, HttpServletRequest request) {
        BaseErrorCode baseErrorCode = ex.getCode();

        sendErrorToSentry(
            ex,
            request.getRequestURI(),
            request.getQueryString(),
            baseErrorCode.getCustomCode()
        );

        return handleExceptionInternal(ex, baseErrorCode, null, request);
    }

    private ResponseEntity<Object> handleExceptionInternal(
        ApplicationException e,
        BaseErrorCode baseErrorCode,
        HttpHeaders headers,
        HttpServletRequest request
    ) {
        ApplicationResponse<Object> body = ApplicationResponse.onFailure(
            baseErrorCode.getCustomCode(),
            baseErrorCode.getMessage(),
            null
        );

        WebRequest webRequest = new ServletWebRequest(request);

        return super.handleExceptionInternal(
            e,
            body,
            headers,
            baseErrorCode.getHttpStatus(),
            webRequest
        );
    }

    private ResponseEntity<Object> handleExceptionInternalFalse(
        Exception e,
        HttpStatus status,
        WebRequest request,
        String errorPoint
    ) {
        ApplicationResponse<Object> body = ApplicationResponse.onFailure(
            CommonErrorCode.INTERNAL_SERVER_ERROR.getCustomCode(),
            CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
            errorPoint
        );

        return super.handleExceptionInternal(
            e,
            body,
            HttpHeaders.EMPTY,
            status,
            request
        );
    }

    private ResponseEntity<Object> handleExceptionInternalArgs(
        Exception e,
        WebRequest request,
        Map<String, String> errorArgs
    ) {
        ApplicationResponse<Object> body = ApplicationResponse.onFailure(
            CommonErrorCode.BAD_REQUEST.getCustomCode(),
            CommonErrorCode.BAD_REQUEST.getMessage(),
            errorArgs
        );

        return super.handleExceptionInternal(
            e,
            body,
            HttpHeaders.EMPTY,
            CommonErrorCode.BAD_REQUEST.getHttpStatus(),
            request
        );
    }

    private ResponseEntity<Object> handleExceptionInternalConstraint(
        Exception e,
        BaseErrorCode baseErrorCode,
        WebRequest request
    ) {
        ApplicationResponse<Object> body = ApplicationResponse.onFailure(
            baseErrorCode.getCustomCode(),
            baseErrorCode.getMessage(),
            null
        );

        return super.handleExceptionInternal(
            e,
            body,
            HttpHeaders.EMPTY,
            baseErrorCode.getHttpStatus(),
            request
        );
    }

    private static void sendErrorToSentry(Exception ex, String requestUri, String queryString, String errorCode) {
        Sentry.withScope(scope -> {
            // Sentry에서 트랜잭션 이름을 요청 URI로 설정
            scope.setTransaction(requestUri);

            scope.setTag("path", requestUri);
            if (errorCode != null && !errorCode.isBlank()) {
                scope.setTag("error.code", errorCode);
                // 같은 에러코드면 같은 이슈로 그룹핑, Sentry에서 표시되는 이슈 제목을 에러 코드 자체로 표시
                scope.setFingerprint(List.of(errorCode));
            }

            if (queryString != null && !queryString.isBlank()) {
                scope.setExtra("query", queryString);
            }

            Sentry.captureException(ex);
        });
    }
}