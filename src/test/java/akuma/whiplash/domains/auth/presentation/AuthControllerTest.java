package akuma.whiplash.domains.auth.presentation;

import static akuma.whiplash.common.fixture.MemberFixture.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.application.dto.request.SocialLoginRequest;
import akuma.whiplash.domains.auth.application.dto.response.LoginResponse;
import akuma.whiplash.domains.auth.application.dto.response.TokenResponse;
import akuma.whiplash.domains.auth.application.usecase.AuthUseCase;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.global.config.security.SecurityConfig;
import akuma.whiplash.global.config.security.jwt.JwtAuthenticationFilter;
import akuma.whiplash.global.exception.ApplicationException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AuthUseCase authUseCase;

    private static final String BASE = "/api/v1/auth";

    private SocialLoginRequest buildSocialLoginRequest(String provider) {
        return new SocialLoginRequest(
            provider,
            "provider-access-token",
            "device-social-login",
            "ANDROID",
            "fcm-social-login",
            "1.0.0",
            "14",
            "Asia/Seoul"
        );
    }

    private MemberContext buildContext(MemberFixture fixture) {
        return MemberContext.builder()
            .memberId(fixture.getId())
            .role(fixture.getRole())
            .provider(fixture.getProvider())
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
    @DisplayName("[POST] /api/auth/social-login - 소셜 로그인")
    class SocialLoginTest {

        @Test
        @DisplayName("성공: 200과 accessToken, refreshToken, member 필드를 반환한다")
        void success() throws Exception {
            // given
            SocialLoginRequest request = buildSocialLoginRequest("MOCK");
            LoginResponse response = LoginResponse.builder()
                .accessToken("Bearer access-token")
                .refreshToken("Bearer refresh-token")
                .member(LoginResponse.MemberInfo.builder()
                    .memberId(1L)
                    .provider("MOCK")
                    .nickname("김민형")
                    .email("kmh@gmail.com")
                    .isNewMember(true)
                    .status("ACTIVE")
                    .build())
                .build();

            org.mockito.Mockito.when(authUseCase.socialLogin(any(SocialLoginRequest.class)))
                .thenReturn(response);

            // when & then
            mockMvc.perform(post(BASE + "/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").value("Bearer access-token"))
                .andExpect(jsonPath("$.result.refreshToken").value("Bearer refresh-token"))
                .andExpect(jsonPath("$.result.member.memberId").value(1L))
                .andExpect(jsonPath("$.result.member.provider").value("MOCK"))
                .andExpect(jsonPath("$.result.member.nickname").value("김민형"))
                .andExpect(jsonPath("$.result.member.email").value("kmh@gmail.com"))
                .andExpect(jsonPath("$.result.member.isNewMember").value(true))
                .andExpect(jsonPath("$.result.member.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("실패: 지원하지 않는 provider이면 400을 반환한다")
        void fail_unsupportedProvider() throws Exception {
            // given
            SocialLoginRequest request = buildSocialLoginRequest("MOCK");
            org.mockito.Mockito.when(authUseCase.socialLogin(any(SocialLoginRequest.class)))
                .thenThrow(ApplicationException.from(AuthErrorCode.UNSUPPORTED_SOCIAL_TYPE));

            // when & then
            mockMvc.perform(post(BASE + "/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.UNSUPPORTED_SOCIAL_TYPE.getCustomCode()));
        }
    }

    @Nested
    @DisplayName("[POST] /api/auth/logout - 로그아웃")
    class LogoutTest {

        @Test
        @DisplayName("성공: 200 OK를 반환한다")
        void success() throws Exception {
            // given
            MemberContext context = buildContext(MEMBER_4);
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
            MemberContext context = buildContext(MEMBER_5);
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
            MemberContext context = buildContext(MEMBER_6);
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
            MemberContext context = buildContext(MEMBER_7);
            setSecurityContext(context);
            doThrow(ApplicationException.from(AuthErrorCode.INVALID_TOKEN))
                .when(authUseCase).logout(context);

            // when & then
            mockMvc.perform(post(BASE + "/logout")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("[POST] /api/v1/auth/token/reissue - 토큰 재발급")
    class ReissueTokenTest {

        @Test
        @DisplayName("성공: 200 OK와 새로운 토큰을 반환한다")
        void success() throws Exception {
            // given
            setSecurityContext(buildContext(MEMBER_1));
            TokenResponse response = TokenResponse.builder()
                .accessToken("Bearer newAccess")
                .refreshToken("Bearer newRefresh")
                .build();
            org.mockito.Mockito.when(authUseCase.reissueToken(any(MemberContext.class)))
                .thenReturn(response);

            // when & then
            mockMvc.perform(post(BASE + "/token/reissue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").value("Bearer newAccess"))
                .andExpect(jsonPath("$.result.refreshToken").value("Bearer newRefresh"));
        }

        @Test
        @DisplayName("실패: 토큰이 유효하지 않으면 401과 에러 코드를 반환한다")
        void fail_invalidToken() throws Exception {
            // given
            setSecurityContext(buildContext(MEMBER_2));
            org.mockito.Mockito.when(authUseCase.reissueToken(any(MemberContext.class)))
                .thenThrow(ApplicationException.from(AuthErrorCode.INVALID_TOKEN));

            // when & then
            mockMvc.perform(post(BASE + "/token/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_TOKEN.getCustomCode()));
        }

        @Test
        @DisplayName("실패: 리프레시 토큰이 존재하지 않으면 401와 에러 코드를 반환한다")
        void fail_tokenNotFound() throws Exception {
            // given
            setSecurityContext(buildContext(MEMBER_3));
            org.mockito.Mockito.when(authUseCase.reissueToken(any(MemberContext.class)))
                .thenThrow(ApplicationException.from(AuthErrorCode.INVALID_TOKEN));

            // when & then
            mockMvc.perform(post(BASE + "/token/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_TOKEN.getCustomCode()));
        }

        @Test
        @DisplayName("실패: 만료된 리프레시 토큰이면 401과 에러 코드를 반환한다")
        void fail_tokenExpired() throws Exception {
            // given
            setSecurityContext(buildContext(MEMBER_4));
            org.mockito.Mockito.when(authUseCase.reissueToken(any(MemberContext.class)))
                .thenThrow(ApplicationException.from(AuthErrorCode.TOKEN_EXPIRED));

            // when & then
            mockMvc.perform(post(BASE + "/token/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.TOKEN_EXPIRED.getCustomCode()));
        }
    }

}
