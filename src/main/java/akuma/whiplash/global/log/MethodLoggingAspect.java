package akuma.whiplash.global.log;

import static akuma.whiplash.global.log.LogConst.MDC_REQUEST_ID_KEY;
import akuma.whiplash.global.log.MethodLogSuppressor; 
import akuma.whiplash.global.log.NoMethodLog;         


import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MethodLoggingAspect {

    private final ObjectMapper objectMapper;

    // Controller, Service 메서드 실행 전후 로깅
    @Around("within(@org.springframework.web.bind.annotation.RestController *) || within(@org.springframework.stereotype.Service *)")
    public Object logAroundMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        // Suppress 모드면 로깅하지 않고 바로 실행(로깅 X)
        if (MethodLogSuppressor.isSuppressed()) {
            return joinPoint.proceed();
        }

        
        Instant methodStartTime = Instant.now();

        Object methodReturnValue = null;
        Throwable caughtException = null;

        try {
            methodReturnValue = joinPoint.proceed(); // 실제 메서드 실행
            return methodReturnValue;
        } catch (Throwable ex) {
            caughtException = ex; // 예외 발생 시 저장
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

            // 로그 데이터 Map 형태로 구성
            Map<String, Object> logDataMap = new LinkedHashMap<>();
            logDataMap.put("type", LogConst.TYPE_METHOD); 
            logDataMap.put("timestamp", LogUtils.nowIso()); 
            logDataMap.put("clientIp", clientIpAddress); 
            logDataMap.put("class", declaringClassName); 
            logDataMap.put("method", methodName); 
            logDataMap.put("params", truncatedParams);
            logDataMap.put("durationMs", executionTimeMillis); 
            logDataMap.put("requestId", MDC.get(MDC_REQUEST_ID_KEY)); // 필터가 설정한 requestId를 함께 남겨 상관관계 분석 용이

            if (caughtException != null) {
                // 예외 발생시 예외 정보 로깅
                logDataMap.put("exception", caughtException.getClass().getName()); 
                logDataMap.put("exceptionMessage", caughtException.getMessage()); 
                log.error(LogUtils.toJson(objectMapper, logDataMap)); 
            } else {
                log.info(LogUtils.toJson(objectMapper, logDataMap)); 
            }
        }
    }


    /**
     * @NoMethodLog 가 붙은 메서드/클래스의 실행 구간 동안
     * 같은 스레드 체인에서 Method 로깅을 억제한다.
     */
    @Around("@within(akuma.whiplash.global.log.NoMethodLog) || @annotation(akuma.whiplash.global.log.NoMethodLog)")
    public Object suppressByAnnotation(ProceedingJoinPoint pjp) throws Throwable {
        boolean prev = MethodLogSuppressor.isSuppressed();
        MethodLogSuppressor.enable();
        try {
            return pjp.proceed();
        } finally {
            // 중첩 억제에 대비: 이전 상태 복원
            if (prev) {
                MethodLogSuppressor.enable();
            } else {
                MethodLogSuppressor.disable();
            }
        }
    }
}
