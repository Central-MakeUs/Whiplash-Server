package akuma.whiplash.global.config.security;

import static akuma.whiplash.domains.member.domain.contants.Role.ADMIN;
import static akuma.whiplash.domains.member.domain.contants.Role.USER;
import static org.springframework.http.HttpMethod.*;

import akuma.whiplash.domains.member.domain.contants.Role;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
public class RequestMatcherHolder {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<RequestInfo> REQUEST_INFO_LIST = List.of(

        // static resources
        new RequestInfo(GET, "/*.ico", null),
        new RequestInfo(GET, "/resources/**", null),
        new RequestInfo(GET, "/css/**", null),
        new RequestInfo(GET, "/js/**", null),
        new RequestInfo(GET, "/img/**", null),


        // auth
        new RequestInfo(POST, "/api/v1/auth/social-login", null),
        new RequestInfo(POST, "/api/v1/auth/**", USER),

        // swagger
        new RequestInfo(GET, "/api/nuntteo/swagger-ui.html", null),              // 진입점
        new RequestInfo(GET, "/api/nuntteo/swagger-ui/**", null),              // UI 리소스들
        new RequestInfo(GET, "/api/nuntteo/v3/api-docs/**", null),              // API docs
        new RequestInfo(GET, "/api/nuntteo/swagger-ui.html", null),              // 진입점
        new RequestInfo(GET, "/swagger-ui/**",null),
        new RequestInfo(GET, "/swagger-ui/index.html",null),
        new RequestInfo(GET, "/v3/api-docs/**",null), // 예비 (경로 누락 방지)
        new RequestInfo(GET, "/swagger-resources/**",null),
        new RequestInfo(GET, "/webjars/**",null),
        new RequestInfo(GET, "/favicon.ico",null),

        // 부하 테스트 전용 (profile: !prod) — prod에서는 Controller Bean 자체가 생성되지 않음
        new RequestInfo(GET, "/api/load-test/**", null),
        new RequestInfo(POST, "/api/load-test/**", null),
        new RequestInfo(DELETE, "/api/load-test/**", null),

        // alarm
        new RequestInfo(GET, "/api/v1/alarms/**",USER),
        new RequestInfo(POST, "/api/v1/alarms/**",USER),
        new RequestInfo(PUT, "/api/v1/alarms/**",USER),
        new RequestInfo(DELETE, "/api/v1/alarms/**",USER),

        // admob
        new RequestInfo(GET, "/api/v1/ads/rewards/callback/admob", null),

        // member
        new RequestInfo(GET, "/api/v1/members/**", USER),
        new RequestInfo(POST, "/api/v1/members/**", USER),
        new RequestInfo(PUT, "/api/v1/members/**", USER),
        new RequestInfo(DELETE, "/api/v1/members/**", USER),

        // device
        new RequestInfo(GET, "/api/v1/devices/**", USER),
        new RequestInfo(POST, "/api/v1/devices/**", USER),
        new RequestInfo(PUT, "/api/v1/devices/**", USER),
        new RequestInfo(DELETE, "/api/v1/devices/**", USER),

        // place
        new RequestInfo(GET, "/api/places/**", USER),

        // actuator
        new RequestInfo(GET, "/actuator/**", null),

        // 로컬 테스트
        new RequestInfo(POST, "/api/dev/auth/login", null),

        // QA 전용 토큰 발급 (SecurityConfig에서 qa 프로파일에서만 등록)
        new RequestInfo(POST, "/qa/auth/token", null),

        // 빌드 에러 방지를 위해 각 권한에 대한 RequestInfo가 최소 1개씩은 리스트에 있어야함
        new RequestInfo(GET, "/api/admin/**", ADMIN)
    );

    /**
     * 최소 권한이 주어진 요청에 대한 RequestMatcher 반환
     * @param minRole 최소 권한 (Nullable)
     * @return 생성된 RequestMatcher
     */
    public String[] getPatternsByMinPermission(@Nullable Role minRole) {
        return REQUEST_INFO_LIST.stream()
            .filter(info -> Objects.equals(info.minRole, minRole))
            .map(RequestInfo::pattern)
            .distinct()
            .toArray(String[]::new);
    }

    private record RequestInfo(HttpMethod method, String pattern, Role minRole) {

    }

    public boolean isPermitAll(String requestUri, String method) {

        return REQUEST_INFO_LIST.stream()
            .filter(info -> info.minRole == null) // 권한이 필요 없는 엔드포인트
            .anyMatch(info ->
                info.method().matches(method) &&
                    PATH_MATCHER.match(info.pattern(), requestUri)
            );
    }
}
