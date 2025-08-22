package akuma.whiplash.global.config.security.jwt;

import static akuma.whiplash.global.config.security.jwt.constants.TokenType.ACCESS;
import static akuma.whiplash.global.config.security.jwt.constants.TokenType.REFRESH;

import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.global.config.security.RequestMatcherHolder;
import akuma.whiplash.global.exception.ApplicationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final RequestMatcherHolder requestMatcherHolder;
    private final Environment env;

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String clientIp = getClientIp(request);

        String[] activeProfiles = env.getActiveProfiles();
        boolean isLocal = Arrays.asList(activeProfiles).contains("local");
        boolean isTest  = Arrays.asList(activeProfiles).contains("test");

        if (isLocal) {
            log.info("요청 IP: {}", clientIp);
        }

        String token = extractToken(request);

        if (token != null) {

            if (isLocal) {
                log.info("토큰 함께 요청 : {}", token);
            }

            try {
                if (request.getRequestURI().contains("/reissue")) {
                    log.info("재발급 진행");
                    jwtUtils.validateToken(response, token, REFRESH);
                } else if (request.getRequestURI().contains("/logout")) {
                    log.info("로그아웃 진행");
                    jwtUtils.validateToken(response, token, REFRESH);
                } else {
                    log.info("일반 접근");
                    jwtUtils.validateToken(response, token, ACCESS);
                }

                Authentication authentication = jwtUtils.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (isLocal) {
                    log.info("context 인증 정보 저장 : {}", authentication.getName());
                }

            } catch (ApplicationException e) {
                return;
            }
        } else {
            jwtUtils.jwtExceptionHandler(response, AuthErrorCode.INVALID_TOKEN);
            return; // 토큰 비어있을 시 필터 체인 중단
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        log.info("Request URI = {}", request.getRequestURI());
        return requestMatcherHolder.isPermitAll(request.getRequestURI(), request.getMethod());
    }

    private String extractToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION);

        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(BEARER)) {
            return authorizationHeader.split(" ")[1];
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 여러 IP가 있을 수 있으므로 첫 번째 꺼냄
            return ip.split(",")[0];
        }

        return request.getRemoteAddr(); // 프록시 없을 경우
    }
}
