package akuma.whiplash.domains.member.presentation;

import static akuma.whiplash.domains.member.exception.MemberErrorCode.MEMBER_NOT_FOUND;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.member.application.usecase.MemberUseCase;
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
            .provider(fixture.getProvider())
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
    @DisplayName("[DELETE] /api/members - 회원 탈퇴")
    class DeleteMemberTest {

        @Test
        @DisplayName("성공: 회원 탈퇴 요청 시 200 OK를 반환한다")
        void success() throws Exception {
            // given
            MemberContext context = buildContextFromFixture(MemberFixture.MEMBER_1);
            setSecurityContext(context);

            // when & then
            mockMvc.perform(delete("/api/v1/members"))
                .andExpect(status().isOk());

            verify(memberUseCase).deleteMember(context);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원이면 404와 에러 코드를 반환한다")
        void fail_memberNotFound() throws Exception {
            // given
            MemberContext context = buildContextFromFixture(MemberFixture.MEMBER_2);
            setSecurityContext(context);
            doThrow(ApplicationException.from(MEMBER_NOT_FOUND))
                .when(memberUseCase).deleteMember(context);

            // when & then
            mockMvc.perform(delete("/api/v1/members"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(MEMBER_NOT_FOUND.getCustomCode()));
        }
    }
}
