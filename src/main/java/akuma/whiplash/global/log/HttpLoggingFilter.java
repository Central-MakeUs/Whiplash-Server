package akuma.whiplash.global.log;

import static akuma.whiplash.global.log.LogConst.MDC_REQUEST_ID_KEY;
import static akuma.whiplash.global.log.LogConst.REQ_ID_HEADER;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 가장 먼저 실행되어 요청/응답을 감싸야 함
@RequiredArgsConstructor
public class HttpLoggingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        // 모니터링 불필요한 경로 제외
        return requestUri.startsWith("/actuator/") || requestUri.startsWith("/health");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse,
        FilterChain filterChain
    ) throws ServletException, IOException {

        // 요청/응답 바디를 캐싱해 재사용 가능하게 래핑
        ContentCachingRequestWrapper cachedRequestWrapper = new ContentCachingRequestWrapper(httpServletRequest);
        ContentCachingResponseWrapper cachedResponseWrapper = new ContentCachingResponseWrapper(httpServletResponse);

        Instant requestStartTime = Instant.now(); // 요청 시작 시간 기록

        // 요청 단위 식별값을 MDC에 저장(동일 요청의 모든 로그에 requestId 자동 부착)
        String requestId = Optional.ofNullable(httpServletRequest.getHeader(REQ_ID_HEADER))
                                   .filter(header -> !header.isBlank())
                                   .orElse(UUID.randomUUID().toString()); // 외부 미전달 시 서버에서 생성
        MDC.put(MDC_REQUEST_ID_KEY, requestId); // ← MDC 저장 (로그 패턴 %X{requestId} 로 노출)
        httpServletResponse.setHeader(REQ_ID_HEADER, requestId); // 클라이언트와 상호 추적을 위해 응답에도 반환

        try {
            filterChain.doFilter(cachedRequestWrapper, cachedResponseWrapper); // 다음 필터/컨트롤러로 전달
        } finally {
            String clientIpAddress = LogUtils.clientIp(httpServletRequest);
            String httpMethod = httpServletRequest.getMethod();
            String requestUri = httpServletRequest.getRequestURI();
            Map<String, String> pathVariableMap = LogUtils.pathVariables(httpServletRequest);
            String queryString = httpServletRequest.getQueryString();

            // 요청 본문 읽기 + 민감정보 마스킹 + 길이 제한
            String requestBody = getRequestBodyFromCache(cachedRequestWrapper);
            String maskedRequestBody = LogUtils.maskSensitiveJson(objectMapper, requestBody);
            String truncatedRequestBody = LogUtils.truncate(maskedRequestBody, LogConst.MAX_BODY_LEN);

            // 응답 본문 읽기 + 민감정보 마스킹
            String responseBody = getResponseBodyFromCache(cachedResponseWrapper);
            String maskedResponseBody = LogUtils.maskSensitiveJson(objectMapper, responseBody);
            Map<String, Object> responseFieldMap = extractResponseFields(maskedResponseBody); 

            // HTTP 상태 코드, 총 처리 시간 계산 
            int httpStatusCode = cachedResponseWrapper.getStatus(); 
            long elapsedTimeMillis = Duration.between(requestStartTime, Instant.now()).toMillis();

            // 로그 데이터 구성(로그 유형, 로그 발생 시각, 요청자 IP)
            Map<String, Object> logDataMap = new LinkedHashMap<>();
            logDataMap.put("type", LogConst.TYPE_HTTP); 
            logDataMap.put("timestamp", LogUtils.nowIso()); 
            logDataMap.put("clientIp", clientIpAddress); 
            logDataMap.put("requestId", requestId); // 추후 검색 편의를 위해 payload에도 포함(선택)

            // 요청 정보
            logDataMap.put("httpMethod", httpMethod);
            logDataMap.put("url", requestUri);
            logDataMap.put("pathVariables", pathVariableMap);
            logDataMap.put("queryString", queryString);
            logDataMap.put("requestBody", truncatedRequestBody);

            // 응답 정보
            logDataMap.put("status", httpStatusCode);
            logDataMap.put("response.code", responseFieldMap.get("code"));
            logDataMap.put("response.message", responseFieldMap.get("message"));
            logDataMap.put("response.result", responseFieldMap.get("result"));
            logDataMap.put("durationMs", elapsedTimeMillis);

            String logJson = LogUtils.toJson(objectMapper, logDataMap); // JSON 포맷으로 변환

            // 상태 코드별 로그 레벨 지정
            if (httpStatusCode >= 500) {
                log.error(logJson);
            } else if (httpStatusCode >= 400) {
                log.warn(logJson);
            } else {
                log.info(logJson);
            }

            // 응답 본문을 실제 클라이언트로 전달
            cachedResponseWrapper.copyBodyToResponse(); 

            MDC.clear(); // 요청 처리 종료 후 MDC 정리(메모리 누수/오염 방지)
        }
    }

    private String getRequestBodyFromCache(ContentCachingRequestWrapper requestWrapper) {
        // 요청 본문 캐시에서 꺼내 문자열로 변환
        byte[] contentBytes = requestWrapper.getContentAsByteArray();
        if (contentBytes.length == 0) {
            return "";
        }
        return new String(contentBytes, StandardCharsets.UTF_8);
    }

    private String getResponseBodyFromCache(ContentCachingResponseWrapper responseWrapper) {
        // 응답 본문 캐시에서 꺼내 문자열로 변환
        byte[] contentBytes = responseWrapper.getContentAsByteArray();
        if (contentBytes.length == 0) {
            return "";
        }
        return new String(contentBytes, StandardCharsets.UTF_8);
    }

    private Map<String, Object> extractResponseFields(String responseJson) {
        // 표준 응답 형식(code/message/result)만 추출
        Map<String, Object> fieldMap = new LinkedHashMap<>();
        fieldMap.put("code", null);
        fieldMap.put("message", null);
        fieldMap.put("result", null);

        if (responseJson == null || responseJson.isBlank()) {
            return fieldMap;
        }

        try {
            Map<String, Object> jsonMap = objectMapper.readValue(responseJson, Map.class);
            fieldMap.put("code", jsonMap.get("code"));
            fieldMap.put("message", jsonMap.get("message"));
            fieldMap.put("result", jsonMap.get("result"));
        } catch (Exception ignored) {}

        return fieldMap;
    }
}
