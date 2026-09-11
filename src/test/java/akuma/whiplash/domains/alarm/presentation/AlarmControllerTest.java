package akuma.whiplash.domains.alarm.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.domains.alarm.application.dto.response.AlarmAdSessionCreateResponse;
import akuma.whiplash.domains.alarm.application.usecase.AlarmUseCase;
import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.global.config.security.SecurityConfig;
import akuma.whiplash.global.config.security.jwt.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AlarmController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AlarmController Slice Test")
class AlarmControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AlarmUseCase alarmUseCase;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("광고 세션 API")
    class AdSessionApiTest {

        @Test
        @DisplayName("성공: 인증된 기기 기준으로 알람 끄기 광고 세션을 발급한다")
        void success() throws Exception {
            // given
            MemberContext context = memberContext();
            setContext(context);
            when(alarmUseCase.createOffAdSession(any(), any(), any(), any()))
                .thenReturn(AlarmAdSessionCreateResponse.builder().adSessionId("ad-session").expiresAt(LocalDateTime.of(2026, 5, 2, 15, 0)).build());

            // when
            mockMvc.perform(post("/api/v1/alarms/10/off/ad-session").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(java.util.Map.of("occurrenceId", 11))))
                .andExpect(status().isOk());

            // then
            verify(alarmUseCase).createOffAdSession(context.memberId(), context.deviceId(), 10L, new akuma.whiplash.domains.alarm.application.dto.request.AlarmOffAdSessionCreateRequest(11L));
        }

        @Test
        @DisplayName("성공: 삭제 세션 발급은 요청 body의 기기 ID 없이 처리한다")
        void success_deleteSession() throws Exception {
            // given
            MemberContext context = memberContext();
            setContext(context);
            when(alarmUseCase.createDeleteAdSession(any(), any(), any()))
                .thenReturn(AlarmAdSessionCreateResponse.builder().adSessionId("ad-session").expiresAt(LocalDateTime.of(2026, 5, 2, 15, 0)).build());

            // when
            mockMvc.perform(post("/api/v1/alarms/10/delete/ad-session"))
                .andExpect(status().isOk());

            // then
            verify(alarmUseCase).createDeleteAdSession(context.memberId(), context.deviceId(), 10L);
        }
    }

    private MemberContext memberContext() {
        return MemberContext.builder().memberId(1L).deviceId("device-uuid").role(akuma.whiplash.domains.member.domain.contants.Role.USER).build();
    }

    private void setContext(MemberContext context) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(context, null, List.of(new SimpleGrantedAuthority(context.role().name()))));
    }
}
