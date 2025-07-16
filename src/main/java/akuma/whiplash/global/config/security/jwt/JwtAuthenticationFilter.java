package akuma.whiplash.global.config.security.jwt;

import static akuma.whiplash.global.config.security.jwt.constants.TokenType.ACCESS;
import static akuma.whiplash.global.config.security.jwt.constants.TokenType.REFRESH;

import akuma.whiplash.global.exception.ApplicationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            log.info("토큰 함께 요청 : {}", token);
            try {
                if (request.getRequestURI().contains("/refresh")) {
                    log.info("재발급 진행");
                    jwtUtils.validateToken(response, token, REFRESH);
                } else {
                    log.info("일반 접근");
                    jwtUtils.validateToken(response, token, ACCESS);
                }

                Authentication authentication = jwtUtils.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("context 인증 정보 저장 : {}", authentication.getName());
            } catch (ApplicationException e) {
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION);

        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(BEARER)) {
            return authorizationHeader.split(" ")[1];
        }
        return null;
    }
}
