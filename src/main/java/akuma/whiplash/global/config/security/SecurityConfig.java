package akuma.whiplash.global.config.security;

import akuma.whiplash.global.config.security.jwt.JwtAuthenticationFilter;
import akuma.whiplash.global.config.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtils jwtUtils;

    private static final String[] STATIC_RESOURCES = {
        "/resource/**",
        "/css/**",
        "/js/**",
        "/img/**",
        "/lib/**",
        "/webjars/**"
    };

    private static final String[] SWAGGER_ENDPOINTS = {
        "/api/noonddu/swagger-ui.html",              // 진입점
        "/api/noonddu/swagger-ui/**",                // UI 리소스들
        "/api/noonddu/v3/api-docs/**",               // API docs
        "/v3/api-docs/**",                            // 예비 (경로 누락 방지)
        "/swagger-ui/**", "/swagger-resources/**"
    };

    private static final String[] PUBLIC_API_ENDPOINTS = {
        "/api/auth/social-login",
        "/api/auth/social-logout"
    };

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
                .requestMatchers(PUBLIC_API_ENDPOINTS).permitAll()
                .requestMatchers(SWAGGER_ENDPOINTS).permitAll()
                .requestMatchers(STATIC_RESOURCES).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtils),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
