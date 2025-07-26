package akuma.whiplash.global.config.security;

import static akuma.whiplash.domains.member.domain.contants.Role.ADMIN;
import static akuma.whiplash.domains.member.domain.contants.Role.USER;

import akuma.whiplash.global.config.security.jwt.JwtAuthenticationFilter;
import akuma.whiplash.global.config.security.jwt.JwtUtils;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtils jwtUtils;
    private final RequestMatcherHolder requestMatcherHolder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(requestMatcherHolder.getPatternsByMinPermission(null)).permitAll()
                .requestMatchers(requestMatcherHolder.getPatternsByMinPermission(USER))
                .hasAnyAuthority(USER.name(), ADMIN.name())
                .requestMatchers(requestMatcherHolder.getPatternsByMinPermission(ADMIN))
                .hasAnyAuthority(ADMIN.name())
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtils, requestMatcherHolder),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
            // 웹용 로컬 테스트용
            "http://localhost:3000",
            "http://localhost:8080",

            // 로컬 네트워크 (안드로이드 에뮬레이터에서 PC 서버 접속 시 IP 필요)
            "http://192.168.0.100:8080",  // 본인 PC의 IP 주소로 교체
            "http://10.0.2.2:8080"       // Android 에뮬레이터에서 Host PC를 가리키는 특별 주소
        ));

        configuration.setAllowedHeaders(Arrays.asList("*"));

        configuration.setAllowedMethods(Arrays.asList(
            "GET",
            "POST",
            "DELETE",
            "PATCH",
            "OPTIONS"
        ));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
