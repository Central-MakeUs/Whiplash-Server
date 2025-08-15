package akuma.whiplash.global.log;

public final class LogConst {
    private LogConst() {}
    public static final String TYPE_METHOD = "METHOD";
    public static final String TYPE_HTTP = "HTTP";
    public static final String ATTR_START_TS = "LOG_START_TS";
    public static final String ATTR_PATH_VARIABLES = "org.springframework.web.servlet.View.pathVariables";
    public static final int MAX_BODY_LEN = 5000;
    public static final String MDC_REQUEST_ID_KEY = "requestId"; // 모든 로그에 자동으로 붙일 키
    public static final String REQ_ID_HEADER = "X-Request-Id";   // 외부와 상호 운용을 위한 헤더명
}