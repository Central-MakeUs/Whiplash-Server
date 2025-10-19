package akuma.whiplash.domains.member.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.config.IntegrationTest;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.member.application.dto.request.MemberPrivacyPolicyModifyRequest;
import akuma.whiplash.domains.member.application.dto.request.MemberPushNotificationPolicyModifyRequest;
import akuma.whiplash.domains.member.domain.contants.Role;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import akuma.whiplash.global.response.code.CommonErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("MemberController Integration Test")
class MemberControllerIntegrationTest {

    @Autowired private JwtProvider jwtProvider;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MemberRepository memberRepository;

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

    private MemberContext buildContextFrom(MemberEntity member) {
        return MemberContext.builder()
            .memberId(member.getId())
            .socialId(member.getSocialId())
            .email(member.getEmail())
            .nickname(member.getNickname())
            .role(member.getRole())
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
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "device");
            MemberPushNotificationPolicyModifyRequest request = MemberPushNotificationPolicyModifyRequest.builder()
                .pushNotificationPolicy(false)
                .build();
            setSecurityContext(buildContextFrom(member));

            // when
            mockMvc.perform(put("/api/members/terms/push-notifications")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            // then
            MemberEntity updated = memberRepository.findById(member.getId()).orElseThrow();
            assertThat(updated.isPushNotificationPolicy()).isFalse();
        }

/*        @Test
        @DisplayName("실패: 회원이 존재하지 않으면 400와 에러 코드를 반환한다")
        void fail_memberNotFound() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "device");
            MemberPushNotificationPolicyModifyRequest request = MemberPushNotificationPolicyModifyRequest.builder()
                .pushNotificationPolicy(false)
                .build();
            setSecurityContext(buildContextFrom(member));


            // when & then
            mockMvc.perform(put("/api/members/terms/push-notifications")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCustomCode()));
        }*/

        @Test
        @DisplayName("실패: 푸시 알림 수신 동의 값이 null이면 400과 에러 코드를 반환한다")
        void fail_invalidRequest() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "device");
            setSecurityContext(buildContextFrom(member));

            // when & then
            mockMvc.perform(put("/api/members/terms/push-notifications")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCustomCode()));
        }
    }

    @Nested
    @DisplayName("[PUT] /api/members/terms/privacy - 개인정보 수집 동의 변경")
    class ModifyPrivacyPolicyTest {

        @Test
        @DisplayName("성공: 개인정보 수집 동의를 변경하고 200 OK를 반환한다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            member.updatePrivacyPolicy(false);
            memberRepository.save(member);
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "device");
            MemberPrivacyPolicyModifyRequest request = MemberPrivacyPolicyModifyRequest.builder()
                .privacyPolicy(true)
                .build();

            // when
            mockMvc.perform(put("/api/members/terms/privacy")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            // then
            MemberEntity updated = memberRepository.findById(member.getId()).orElseThrow();
            assertThat(updated.isPrivacyPolicy()).isTrue();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원이면 400를 반환한다")
        void fail_memberNotFound() throws Exception {
            // given
            String accessToken = jwtProvider.generateAccessToken(999L, Role.USER, "device");
            MemberPrivacyPolicyModifyRequest request = MemberPrivacyPolicyModifyRequest.builder()
                .privacyPolicy(true)
                .build();

            // when & then
            mockMvc.perform(put("/api/members/terms/privacy")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 요청 값이 null이면 400을 반환한다")
        void fail_invalidRequest() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole(), "device");

            // when & then
            mockMvc.perform(put("/api/members/terms/privacy")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest());
        }
    }
}