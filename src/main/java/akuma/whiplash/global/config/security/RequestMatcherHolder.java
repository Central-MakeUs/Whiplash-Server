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
        new RequestInfo(POST, "/api/auth/social-login", null),
        new RequestInfo(POST, "/api/auth/logout", USER),
        new RequestInfo(POST, "/api/auth/reissue", USER),

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

        // alarm
        new RequestInfo(GET, "/api/alarms/**",USER),
        new RequestInfo(POST, "/api/alarms/**",USER),
        new RequestInfo(PUT, "/api/alarms/**",USER),
        new RequestInfo(DELETE, "/api/alarms/**",USER),

        // member
        new RequestInfo(GET, "/api/members/**", USER),
        new RequestInfo(POST, "/api/members/**", USER),
        new RequestInfo(PUT, "/api/members/**", USER),
        new RequestInfo(DELETE, "/api/members/**", USER),

         // place
         new RequestInfo(GET, "/api/places/**", USER),

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