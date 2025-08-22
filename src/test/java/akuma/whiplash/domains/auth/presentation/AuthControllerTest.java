package akuma.whiplash.domains.auth.presentation;

import static akuma.whiplash.common.fixture.MemberFixture.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.application.dto.request.RegisterFcmTokenRequest;
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
    @DisplayName("[POST] /api/auth/reissue - 토큰 재발급")
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
            mockMvc.perform(post("/api/auth/reissue"))
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
            mockMvc.perform(post("/api/auth/reissue"))
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
            mockMvc.perform(post("/api/auth/reissue"))
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
            mockMvc.perform(post("/api/auth/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.TOKEN_EXPIRED.getCustomCode()));
        }
    }

    @Nested
    @DisplayName("[POST] /api/auth/fcm-token - FCM 토큰 등록")
    class RegisterFcmTokenTest {

        @Test
        @DisplayName("성공: 토큰 등록 후 200 OK를 반환한다")
        void success() throws Exception {
            // given
            RegisterFcmTokenRequest request = new RegisterFcmTokenRequest("token-123");
            setSecurityContext(buildContext(MEMBER_1)); // ✅ 통일

            // when & then
            mockMvc.perform(post(BASE + "/fcm-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            // then
            verify(authUseCase, times(1))
                .registerFcmToken(eq(MEMBER_1.getId()), eq("mock_device"), eq("token-123"));
        }
        @Test
        @DisplayName("실패: FCM 토큰이 비어 있으면 400과 에러 코드를 반환한다")
        void fail_blankToken() throws Exception {
            // given
            RegisterFcmTokenRequest request = new RegisterFcmTokenRequest("");
            setSecurityContext(buildContext(MEMBER_2));

            // when & then
            mockMvc.perform(post(BASE + "/fcm-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            // then
            verify(authUseCase, never()).registerFcmToken(anyLong(), eq("mock_device_id"), eq(""));
        }
    }
}