package akuma.whiplash.domains.auth.presentation;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.application.usecase.AuthUseCase;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.global.config.security.SecurityConfig;
import akuma.whiplash.global.config.security.jwt.JwtAuthenticationFilter;
import akuma.whiplash.global.exception.ApplicationException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("AuthController Slice Test")
@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            SecurityConfig.class,
            JwtAuthenticationFilter.class
        })
    }
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AuthUseCase authUseCase;

    private static final String BASE = "/api/auth";

    private MemberContext buildContext(MemberFixture fixture) {
        return MemberContext.builder()
            .memberId(fixture.getId())
            .role(fixture.getRole())
            .socialId(fixture.getSocialId())
            .email(fixture.getEmail())
            .nickname(fixture.getNickname())
            .deviceId("mock_device")
            .build();
    }

    private void setSecurityContext(MemberContext context) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            context,
            null,
            List.of(new SimpleGrantedAuthority(context.role().name()))
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("[POST] /api/auth/logout - 로그아웃")
    class LogoutTest {

        @Test
        @DisplayName("성공: 200 OK를 반환한다")
        void success() throws Exception {
            // given
            MemberContext context = buildContext(MemberFixture.MEMBER_4);
            setSecurityContext(context);

            // when & then
            mockMvc.perform(post(BASE + "/logout")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            verify(authUseCase, times(1)).logout(context);
        }

        @Test
        @DisplayName("실패: 유효하지 않은 토큰이면 401과 에러 코드를 반환한다")
        void fail_invalidToken() throws Exception {
            // given
            MemberContext context = buildContext(MemberFixture.MEMBER_5);
            setSecurityContext(context);
            doThrow(ApplicationException.from(AuthErrorCode.INVALID_TOKEN))
                .when(authUseCase).logout(context);

            // when & then
            mockMvc.perform(post(BASE + "/logout")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 만료된 토큰이면 401과 에러 코드를 반환한다")
        void fail_tokenExpired() throws Exception {
            // given
            MemberContext context = buildContext(MemberFixture.MEMBER_6);
            setSecurityContext(context);
            doThrow(ApplicationException.from(AuthErrorCode.TOKEN_EXPIRED))
                .when(authUseCase).logout(context);

            // when & then
            mockMvc.perform(post(BASE + "/logout")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 저장되지 않은 토큰이면 401와 에러 코드를 반환한다")
        void fail_tokenNotFound() throws Exception {
            // given
            MemberContext context = buildContext(MemberFixture.MEMBER_7);
            setSecurityContext(context);
            doThrow(ApplicationException.from(AuthErrorCode.INVALID_TOKEN))
                .when(authUseCase).logout(context);

            // when & then
            mockMvc.perform(post(BASE + "/logout")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        }
    }
}