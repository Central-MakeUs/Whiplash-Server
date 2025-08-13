package akuma.whiplash.domains.alarm.presentation;

import static akuma.whiplash.common.fixture.MemberFixture.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.usecase.AlarmUseCase;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.global.config.security.SecurityConfig;
import akuma.whiplash.global.config.security.jwt.JwtAuthenticationFilter;
import akuma.whiplash.global.exception.ApplicationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
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


@WebMvcTest(
    controllers = AlarmController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            SecurityConfig.class,
            JwtAuthenticationFilter.class
        })
    }
)
@AutoConfigureMockMvc(addFilters = false)
class AlarmControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private AlarmUseCase alarmUseCase;

    @DisplayName("알람 등록 요청이 성공하면 200을 반환한다")
    @Test
    void createAlarm_returnsOk_whenRequestValid() throws Exception {
        // given
        AlarmFixture fixture = AlarmFixture.ALARM_03;
        AlarmRegisterRequest request = new AlarmRegisterRequest(
            fixture.getAddress(),
            fixture.getLatitude(),
            fixture.getLongitude(),
            fixture.getAlarmPurpose(),
            fixture.getTime(),
            fixture.getRepeatDays().stream().map(Weekday::getDescription).toList(),
            fixture.getSoundType().getDescription()
        );
        MemberContext context = MemberContext.builder()
            .memberId(MEMBER_3.getId())
            .role(MEMBER_3.getRole())
            .socialId(MEMBER_3.getSocialId())
            .email(MEMBER_3.getEmail())
            .nickname(MEMBER_3.getNickname())
            .deviceId("mock_device_id")
            .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            context, null, List.of(new SimpleGrantedAuthority(MEMBER_3.getRole().name())));

        // Filter exclude 했으므로 @AuthenticationPrincipal MemberContext를 가져오기 위해 SecurityContext 설정
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        doNothing().when(alarmUseCase).createAlarm(any(), any(Long.class));

        // when & then
        mockMvc.perform(post("/api/alarms")
                .with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @DisplayName("회원이 존재하지 않으면 404를 반환한다")
    @Test
    void createAlarm_returnsNotFound_whenMemberNotExists() throws Exception {
        // given
        AlarmFixture fixture = AlarmFixture.ALARM_04;
        AlarmRegisterRequest request = new AlarmRegisterRequest(
            fixture.getAddress(),
            fixture.getLatitude(),
            fixture.getLongitude(),
            fixture.getAlarmPurpose(),
            fixture.getTime(),
            fixture.getRepeatDays().stream().map(Weekday::getDescription).toList(),
            fixture.getSoundType().getDescription()
        );
        MemberContext context = MemberContext.builder()
            .memberId(MEMBER_4.getId())
            .role(MEMBER_4.getRole())
            .socialId(MEMBER_4.getSocialId())
            .email(MEMBER_4.getEmail())
            .nickname(MEMBER_4.getNickname())
            .deviceId("mock_device_id")
            .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            context, null, List.of(new SimpleGrantedAuthority(MEMBER_4.getRole().name())));

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        doThrow(ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND))
            .when(alarmUseCase).createAlarm(any(), any(Long.class));

        // when & then
        mockMvc.perform(post("/api/alarms")
                .with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }
}