package akuma.whiplash.domains.alarm.presentation;

import static akuma.whiplash.common.fixture.MemberFixture.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import akuma.whiplash.domains.alarm.application.usecase.AlarmUseCase;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.exception.AlarmErrorCode;
import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
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
@DisplayName("AlarmController Slice Test")
class AlarmControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private AlarmUseCase alarmUseCase;

    private void setSecurityContext(MemberContext context) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            context,
            null,
            List.of(new SimpleGrantedAuthority(context.role().name()))
        );

        // Filter exclude 했으므로 @AuthenticationPrincipal MemberContext를 가져오기 위해 SecurityContext 설정
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    private MemberContext buildContext(MemberFixture fixture) {
        return MemberContext.builder()
            .memberId(fixture.getId())
            .role(fixture.getRole())
            .socialId(fixture.getSocialId())
            .email(fixture.getEmail())
            .nickname(fixture.getNickname())
            .deviceId("mock_device_id")
            .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    @Nested
    @DisplayName("[POST] /api/alarms - 알람 등록")
    class CreateAlarmTest {

        @Test
        @DisplayName("성공: 알람 등록 요청이 성공하면 200을 반환한다")
        void success() throws Exception {

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
            setSecurityContext(buildContext(MEMBER_3));
            CreateAlarmResponse response = CreateAlarmResponse.builder().alarmId(123L).build();

            // when
            when(alarmUseCase.createAlarm(any(AlarmRegisterRequest.class), anyLong())).thenReturn(response);

            mockMvc.perform(post("/api/alarms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            // then
            verify(alarmUseCase, times(1)).createAlarm(any(AlarmRegisterRequest.class), eq(MEMBER_3.getId()));
        }

        @Test
        @DisplayName("실패: 회원이 존재하지 않으면 404를 반환한다")
        void fail_memberNotFound() throws Exception {

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
            setSecurityContext(buildContext(MEMBER_4));

            // when & then
            when(alarmUseCase.createAlarm(any(AlarmRegisterRequest.class), anyLong()))
                .thenThrow(ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));

            mockMvc.perform(post("/api/alarms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("[POST] /api/alarms/{id}/ring - 알람 울림")
    class RingAlarmTest {

        @Test
        @DisplayName("성공: 알람이 울리면 200을 반환한다")
        void success() throws Exception {

            // given
            setSecurityContext(buildContext(MEMBER_3));

            // when
            mockMvc.perform(post("/api/alarms/{alarmId}/ring", 1L))
                .andExpect(status().isOk());

            // then
            verify(alarmUseCase, times(1)).ringAlarm(eq(MEMBER_3.getId()), eq(1L));
        }

        @Test
        @DisplayName("실패: 알람 시간이 아니면 400을 반환한다")
        void fail_notAlarmTime() throws Exception {

            // given
            setSecurityContext(buildContext(MEMBER_3));

            // when
            doThrow(ApplicationException.from(AlarmErrorCode.NOT_ALARM_TIME))
                .when(alarmUseCase)
                .ringAlarm(eq(MEMBER_3.getId()), eq(1L));

            // then
            mockMvc.perform(post("/api/alarms/{alarmId}/ring", 1L))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("[POST] /api/alarms/{alarmId}/checkin - 도착 인증")
    class CheckinTest {

        @Test
        @DisplayName("성공: 도착 인증 요청이 성공하면 200을 반환한다")
        void success() throws Exception {
            // given
            setSecurityContext(buildContext(MemberFixture.MEMBER_8));
            AlarmCheckinRequest request = new AlarmCheckinRequest(37.0, 127.0);

            // when & then
            mockMvc.perform(post("/api/alarms/{alarmId}/checkin", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            verify(alarmUseCase, times(1))
                .checkinAlarm(eq(MemberFixture.MEMBER_8.getId()), eq(1L), any(AlarmCheckinRequest.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 알람이면 404를 반환한다")
        void fail_alarmNotFound() throws Exception {
            // given
            setSecurityContext(buildContext(MemberFixture.MEMBER_9));
            doThrow(ApplicationException.from(AlarmErrorCode.ALARM_NOT_FOUND))
                .when(alarmUseCase)
                .checkinAlarm(anyLong(), anyLong(), any(AlarmCheckinRequest.class));
            AlarmCheckinRequest request = new AlarmCheckinRequest(37.0, 127.0);

            // when & then
            mockMvc.perform(post("/api/alarms/{alarmId}/checkin", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패: 다른 사용자의 알람이면 403을 반환한다")
        void fail_permissionDenied() throws Exception {
            // given
            setSecurityContext(buildContext(MemberFixture.MEMBER_10));
            doThrow(ApplicationException.from(AuthErrorCode.PERMISSION_DENIED))
                .when(alarmUseCase)
                .checkinAlarm(anyLong(), anyLong(), any(AlarmCheckinRequest.class));
            AlarmCheckinRequest request = new AlarmCheckinRequest(37.0, 127.0);

            // when & then
            mockMvc.perform(post("/api/alarms/{alarmId}/checkin", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패: 이미 도착 인증된 알람이면 400을 반환한다")
        void fail_alreadyDeactivated() throws Exception {
            // given
            setSecurityContext(buildContext(MemberFixture.MEMBER_11));
            doThrow(ApplicationException.from(AlarmErrorCode.ALREADY_DEACTIVATED))
                .when(alarmUseCase)
                .checkinAlarm(anyLong(), anyLong(), any(AlarmCheckinRequest.class));
            AlarmCheckinRequest request = new AlarmCheckinRequest(37.0, 127.0);

            // when & then
            mockMvc.perform(post("/api/alarms/{alarmId}/checkin", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 허용 반경 밖이면 400을 반환한다")
        void fail_outOfRange() throws Exception {
            // given
            setSecurityContext(buildContext(MemberFixture.MEMBER_12));
            doThrow(ApplicationException.from(AlarmErrorCode.CHECKIN_OUT_OF_RANGE))
                .when(alarmUseCase)
                .checkinAlarm(anyLong(), anyLong(), any(AlarmCheckinRequest.class));
            AlarmCheckinRequest request = new AlarmCheckinRequest(37.0, 127.0);

            // when & then
            mockMvc.perform(post("/api/alarms/{alarmId}/checkin", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 다음 주 알람에는 도착 인증할 수 없다")
        void fail_nextWeek() throws Exception {
            // given
            setSecurityContext(buildContext(MemberFixture.MEMBER_13));
            doThrow(ApplicationException.from(AlarmErrorCode.NEXT_WEEK_ALARM_DEACTIVATION_NOT_ALLOWED))
                .when(alarmUseCase)
                .checkinAlarm(anyLong(), anyLong(), any(AlarmCheckinRequest.class));
            AlarmCheckinRequest request = new AlarmCheckinRequest(37.0, 127.0);

            // when & then
            mockMvc.perform(post("/api/alarms/{alarmId}/checkin", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }
}
