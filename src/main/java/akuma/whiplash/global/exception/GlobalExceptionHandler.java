package akuma.whiplash.global.exception;

import akuma.whiplash.global.response.ApplicationResponse;
import akuma.whiplash.global.response.code.BaseErrorCode;
import akuma.whiplash.global.response.code.CommonErrorCode;
import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
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

        Sentry.captureException(e);

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

        Sentry.captureException(e);

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

        Sentry.captureException(ex);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<Object> exception(Exception e, WebRequest request) {
        log.error("Unexpected error: ", e);

        Sentry.captureException(e);

        return handleExceptionInternalFalse(
            e,
            CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus(),
            request,
            e.getMessage()
        );
    }

    @ExceptionHandler(value = ApplicationException.class)
    public ResponseEntity<Object> onThrowException(ApplicationException applicationException, HttpServletRequest request) {
        BaseErrorCode baseErrorCode = applicationException.getCode();

        Sentry.captureException(applicationException);

        return handleExceptionInternal(applicationException, baseErrorCode, null, request);
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
}