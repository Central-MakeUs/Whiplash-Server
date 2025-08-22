package akuma.whiplash.global.config.security.jwt;

import static akuma.whiplash.domains.auth.exception.AuthErrorCode.INVALID_TOKEN;
import static akuma.whiplash.domains.auth.exception.AuthErrorCode.TOKEN_EXPIRED;
import static akuma.whiplash.domains.auth.exception.AuthErrorCode.TOKEN_NOT_FOUND;
import static akuma.whiplash.global.config.security.jwt.constants.TokenType.REFRESH;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.application.mapper.AuthMapper;
import akuma.whiplash.domains.member.domain.service.MemberQueryService;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.global.config.security.jwt.constants.TokenType;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.ApplicationResponse;
import akuma.whiplash.global.response.code.BaseErrorCode;
import akuma.whiplash.infrastructure.redis.RedisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtils {

    private final RedisRepository redisRepository;
    private final MemberQueryService memberQueryService;

    @Value("${jwt.secret-key}")
    private String secret;

    private static final String CONTENT_TYPE = "application/json";
    private static final String CHARACTER_ENCODING = "UTF-8";
    private static final String ROLE = "role";
    private static final String TYPE = "type";

    public void validateToken(HttpServletResponse response, String token, TokenType tokenType) {
        try {
            Claims claims = parseClaims(token);
            if (!claims.get(TYPE).equals(tokenType.name())) {
                throw ApplicationException.from(INVALID_TOKEN);
            }

            if (tokenType.equals(REFRESH)) {
                Long memberId = getMemberIdFromToken(claims);
                String deviceId = getDeviceIdFromToken(claims);
                validateRefreshTokenExists(token, memberId, deviceId);
            }
        } catch (ExpiredJwtException e) {
            jwtExceptionHandler(response, TOKEN_EXPIRED);
            throw ApplicationException.from(TOKEN_EXPIRED);
        } catch (Exception e) {
            jwtExceptionHandler(response, INVALID_TOKEN);
            throw ApplicationException.from(INVALID_TOKEN);
        }
    }

    public void validateRefreshTokenExists(String refreshTokenFromClient, Long memberId, String deviceId) {
        String refreshTokenInRedis = redisRepository
            .getValues(REFRESH + ":" + memberId + ":" + deviceId)
            .orElseThrow(() -> ApplicationException.from(TOKEN_NOT_FOUND));

        // 재사용/탈취 의심 → 정책에 따라 해당 키 삭제, 전체 디바이스 무효화도 가능
        if (!refreshTokenInRedis.equals(refreshTokenFromClient)) {
            throw ApplicationException.from(INVALID_TOKEN);
        }
    }

    public void expireRefreshToken(Long memberId, String deviceId) {
        redisRepository.deleteValues(REFRESH + ":" + memberId + ":" + deviceId);
    }

    public Authentication getAuthentication(String token) throws ApplicationException {
        Claims claims = parseClaims(token);
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(claims.get(ROLE).toString()));

        Long memberId = getMemberIdFromToken(claims);
        String deviceId = getDeviceIdFromToken(claims);
        MemberEntity member = memberQueryService.findById(memberId);
        MemberContext memberContext = AuthMapper.mapToMemberContext(member, deviceId);

        return new UsernamePasswordAuthenticationToken(memberContext, "", authorities);
    }

    public void jwtExceptionHandler(HttpServletResponse response, BaseErrorCode error) {
        response.setStatus(error.getHttpStatus().value());
        response.setContentType(CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);

        log.warn("errorCode {}, errorMessage {}", error.getCustomCode(), error.getMessage());

        try {
            String json = new ObjectMapper().writeValueAsString(ApplicationResponse.onFailure(error.getCustomCode(), error.getMessage()));
            response.getWriter().write(json);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private static Long getMemberIdFromToken(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    private static String getDeviceIdFromToken(Claims claims) {
        return claims.get("deviceId", String.class);
    }
}