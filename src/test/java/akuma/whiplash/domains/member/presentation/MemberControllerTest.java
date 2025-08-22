package akuma.whiplash.domains.member.presentation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.member.application.dto.request.MemberPrivacyPolicyModifyRequest;
import akuma.whiplash.domains.member.application.dto.request.MemberPushNotificationPolicyModifyRequest;
import akuma.whiplash.domains.member.application.usecase.MemberUseCase;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.global.config.security.SecurityConfig;
import akuma.whiplash.global.config.security.jwt.JwtAuthenticationFilter;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.code.CommonErrorCode;
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

@DisplayName("MemberController Slice Test")
@WebMvcTest(
    controllers = MemberController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            SecurityConfig.class,
            JwtAuthenticationFilter.class
        })
    }
)
@AutoConfigureMockMvc(addFilters = false)
class MemberControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private MemberUseCase memberUseCase;

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

    private MemberContext buildContextFromFixture(MemberFixture fixture) {
        return MemberContext.builder()
            .memberId(fixture.getId())
            .socialId(fixture.getSocialId())
            .email(fixture.getEmail())
            .nickname(fixture.getNickname())
            .role(fixture.getRole())
            .deviceId("mock_device_id")
            .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("[PUT] /api/members/terms/push-notifications - 회원 푸시 알림 수신 동의 변경")
    class ModifyPushNotificationPolicyTest {

        @Test
        @DisplayName("성공: 푸시 알림 수신 동의를 변경하면 200 OK를 반환한다")
        void success() throws Exception {
            // given
            MemberPushNotificationPolicyModifyRequest request = MemberPushNotificationPolicyModifyRequest.builder()
                .pushNotificationPolicy(false)
                .build();
            setSecurityContext(buildContextFromFixture(MemberFixture.MEMBER_6));

            // when & then
            mockMvc.perform(put("/api/members/terms/push-notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            verify(memberUseCase).modifyMemberPushNotificationPolicy(eq(MemberFixture.MEMBER_6.getId()), any(MemberPushNotificationPolicyModifyRequest.class));
        }

        @Test
        @DisplayName("실패: 회원이 존재하지 않으면 404와 에러 코드를 반환한다")
        void fail_memberNotFound() throws Exception {
            // given
            MemberPushNotificationPolicyModifyRequest request = MemberPushNotificationPolicyModifyRequest.builder()
                .pushNotificationPolicy(false)
                .build();
            setSecurityContext(buildContextFromFixture(MemberFixture.MEMBER_7));

            doThrow(ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND))
                .when(memberUseCase)
                .modifyMemberPushNotificationPolicy(eq(MemberFixture.MEMBER_7.getId()), any(MemberPushNotificationPolicyModifyRequest.class));

            // when & then
            mockMvc.perform(put("/api/members/terms/push-notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(MemberErrorCode.MEMBER_NOT_FOUND.getCustomCode()));
        }

        @Test
        @DisplayName("실패: 푸시 알림 수신 동의 값이 null이면 400과 에러 코드를 반환한다")
        void fail_invalidRequest() throws Exception {
            // given
            setSecurityContext(buildContextFromFixture(MemberFixture.MEMBER_8));

            // when & then
            mockMvc.perform(put("/api/members/terms/push-notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCustomCode()));

            verify(memberUseCase, never()).modifyMemberPushNotificationPolicy(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("[PUT] /api/members/terms/privacy - 개인정보 수집 동의 변경")
    class ModifyPrivacyPolicyTest {

        @Test
        @DisplayName("성공: 개인정보 수집 동의 변경 후 200 OK를 반환한다")
        void success() throws Exception {
            // given
            MemberContext context = buildContextFromFixture(MemberFixture.MEMBER_1);
            setSecurityContext(context);
            MemberPrivacyPolicyModifyRequest request = MemberPrivacyPolicyModifyRequest.builder()
                .privacyPolicy(true)
                .build();

            // when & then
            mockMvc.perform(put("/api/members/terms/privacy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            verify(memberUseCase, times(1))
                .modifyMemberPrivacyPolicy(eq(context.memberId()), any(MemberPrivacyPolicyModifyRequest.class));
        }

        @Test
        @DisplayName("실패: 회원이 없으면 404와 에러 코드를 반환한다")
        void fail_memberNotFound() throws Exception {
            // given
            MemberContext context = buildContextFromFixture(MemberFixture.MEMBER_2);
            setSecurityContext(context);
            MemberPrivacyPolicyModifyRequest request = MemberPrivacyPolicyModifyRequest.builder()
                .privacyPolicy(true)
                .build();
            doThrow(ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND))
                .when(memberUseCase)
                .modifyMemberPrivacyPolicy(eq(context.memberId()), any(MemberPrivacyPolicyModifyRequest.class));

            // when & then
            mockMvc.perform(put("/api/members/terms/privacy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패: 요청 값이 null이면 400과 에러 코드를 반환한다")
        void fail_invalidRequest() throws Exception {
            // given
            MemberContext context = buildContextFromFixture(MemberFixture.MEMBER_3);
            setSecurityContext(context);

            // when & then
            mockMvc.perform(put("/api/members/terms/privacy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest());
        }
    }
}