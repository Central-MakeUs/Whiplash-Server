package akuma.whiplash.global.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.web.servlet.HandlerMapping;

public final class LogUtils {
    private LogUtils() {}

    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "password", "passwd", "pwd", "authorization", "accessToken", "refreshToken",
        "token", "secret", "apiKey", "privateKey", "email", "phone", "ssn", "address",
        "deviceId", "clientId", "sessionId"
    );

    public static String nowIso() {
        return Instant.now().toString();
    }

    public static String clientIp(HttpServletRequest req) {
        // 1) RFC 7239 Forwarded: for=192.0.2.60;proto=https;by=203.0.113.43
        String fwd = req.getHeader("Forwarded");
        if (hasText(fwd)) {
            String ip = parseForwardedFor(fwd); // for="1.2.3.4" 추출 (쌍따옴표/[] 제거)
            if (hasText(ip)) return ip;
        }

        // 2) X-Forwarded-For: client, proxy1, proxy2...
        String xff = req.getHeader("X-Forwarded-For");
        if (hasText(xff)) {
            String ip = xff.split(",")[0].trim();
            if (hasText(ip)) return ip;
        }

        // 3) X-Real-IP
        String xri = req.getHeader("X-Real-IP");
        if (hasText(xri)) return xri;

        // 4) fallback
        return req.getRemoteAddr();
    }

    private static boolean hasText(String s) { return s != null && !s.isBlank(); }

    private static String parseForwardedFor(String forwarded) {
        // 아주 단순한 파서 (필요시 정교화)
        for (String part : forwarded.split(";")) {
            String p = part.trim();
            if (p.toLowerCase().startsWith("for=")) {
                String v = p.substring(4).trim();
                // for="1.2.3.4" or for=1.2.3.4 or for="[2001:db8::1]"
                v = v.replaceAll("^\"|\"$", "");          // 양끝 따옴표 제거
                v = v.replaceAll("^\\[|\\]$", "");        // IPv6 대괄호 제거
                return v;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> pathVariables(HttpServletRequest request) {
        Object attr = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (attr instanceof Map<?,?> m) {
            Map<String, String> out = new LinkedHashMap<>();
            m.forEach((k, v) -> out.put(String.valueOf(k), String.valueOf(v)));
            return out;
        }
        return Collections.emptyMap();
    }

    public static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...(truncated)";
    }

    public static String toJson(ObjectMapper om, Object v) {
        try { return om.writeValueAsString(v); }
        catch (JsonProcessingException e) { return String.valueOf(v); }
    }

    /**
     * JSON 문자열이면 민감 키 마스킹, 아니면 원본 반환
     */
    public static String maskSensitiveJson(ObjectMapper om, String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        try {
            Object parsed = om.readValue(json, Object.class);
            Object masked = maskRecursive(parsed);
            return om.writeValueAsString(masked);
        } catch (Exception ignore) {
            return json;
        }
    }

    public static String maskSensitiveQuery(String query) {
        if (query == null || query.isBlank()) return query;
        try {
            String[] parts = query.split("&");
            StringBuilder sb = new StringBuilder(query.length());
            for (int i = 0; i < parts.length; i++) {
                String p = parts[i];
                int eq = p.indexOf('=');
                if (eq < 0) {
                    sb.append(p);
                } else {
                    String key = p.substring(0, eq);
                    String val = p.substring(eq + 1);
                    String masked = SENSITIVE_KEYS.contains(key.toLowerCase(Locale.ROOT)) ? "****" : val;
                    sb.append(key).append('=').append(masked);
                }
                if (i + 1 < parts.length) sb.append('&');
            }
            return sb.toString();
        } catch (Exception ignore) {
            return query;
        }
    }

    @SuppressWarnings("unchecked")
    private static Object maskRecursive(Object node) {
        if (node instanceof Map<?,?> m) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?,?> e : m.entrySet()) {
                String k = String.valueOf(e.getKey());
                Object v = e.getValue();
                if (SENSITIVE_KEYS.contains(k)) {
                    copy.put(k, "****");
                } else {
                    copy.put(k, maskRecursive(v));
                }
            }
            return copy;
        } else if (node instanceof List<?> l) {
            List<Object> copy = new ArrayList<>(l.size());
            for (Object v : l) {
                copy.add(maskRecursive(v));
            }
            return copy;
        }
        return node;
    }
}
