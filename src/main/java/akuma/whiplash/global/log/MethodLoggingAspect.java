package akuma.whiplash.global.log;

import static akuma.whiplash.global.log.LogConst.MDC_REQUEST_ID_KEY;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import akuma.whiplash.global.log.MethodLogSuppressor;
import akuma.whiplash.global.log.NoMethodLog;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MethodLoggingAspect {

    private final ObjectMapper objectMapper;

    // Controller, Service 메서드 실행 전후 로깅
    @Around("within(@org.springframework.web.bind.annotation.RestController *) || within(@org.springframework.stereotype.Service *)")
    public Object logAroundMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean noLog = hasNoMethodLog(joinPoint);
        boolean previousSuppressed = MethodLogSuppressor.isSuppressed();
        if (noLog) {
            MethodLogSuppressor.enable();
        }

        if (previousSuppressed || noLog) {
            try {
                return joinPoint.proceed();
            } finally {
                if (noLog && !previousSuppressed) {
                    MethodLogSuppressor.disable();
                }
            }
        }

        Instant methodStartTime = Instant.now();
        Object methodReturnValue = null;
        Throwable caughtException = null;

        try {
            methodReturnValue = joinPoint.proceed();
            return methodReturnValue;
        } catch (Throwable ex) {
            caughtException = ex;
            throw ex;
        } finally {
            long executionTimeMillis = Duration.between(methodStartTime, Instant.now()).toMillis();

            String declaringClassName = joinPoint.getSignature().getDeclaringTypeName();
            String methodName = joinPoint.getSignature().getName();
            Object[] methodArguments = joinPoint.getArgs();
            String methodArgumentsJson = LogUtils.toJson(objectMapper, methodArguments);
            String maskedParams = LogUtils.maskSensitiveJson(objectMapper, methodArgumentsJson);
            String truncatedParams = LogUtils.truncate(maskedParams, LogConst.MAX_BODY_LEN);

            String clientIpAddress = null;
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                clientIpAddress = LogUtils.clientIp(requestAttributes.getRequest());
            }

            Map<String, Object> logDataMap = new LinkedHashMap<>();
            logDataMap.put("type", LogConst.TYPE_METHOD);
            logDataMap.put("timestamp", LogUtils.nowIso());
            logDataMap.put("clientIp", clientIpAddress);
            logDataMap.put("class", declaringClassName);
            logDataMap.put("method", methodName);
            logDataMap.put("params", truncatedParams);
            logDataMap.put("durationMs", executionTimeMillis);
            logDataMap.put("requestId", MDC.get(MDC_REQUEST_ID_KEY));

            if (caughtException != null) {
                logDataMap.put("exception", caughtException.getClass().getName());
                logDataMap.put("exceptionMessage", caughtException.getMessage());
                log.error(LogUtils.toJson(objectMapper, logDataMap));
            } else {
                log.info(LogUtils.toJson(objectMapper, logDataMap));
            }
        }
    }

    private boolean hasNoMethodLog(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return AnnotationUtils.findAnnotation(method, NoMethodLog.class) != null
            || AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), NoMethodLog.class) != null;
    }
}
