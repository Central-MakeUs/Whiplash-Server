package akuma.whiplash.domains.alarm.presentation;

import static akuma.whiplash.common.fixture.MemberFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRemoveRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmPreviewDto;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmSyncItemDto;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmSyncResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import akuma.whiplash.domains.alarm.application.dto.response.GetAlarmsResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

    private static final String BASE = "/api/v1/alarms";

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
            .provider(fixture.getProvider())
            .email(fixture.getEmail())
            .nickname(fixture.getNickname())
            .deviceId("mock_device_id")
            .build();
    }

    private AlarmCheckinRequest buildCheckinRequest() {
        return new AlarmCheckinRequest(501L, "device-uuid", 37.0, 127.0, LocalDateTime.now());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    @Nested
    @DisplayName("[POST] /api/v1/alarms - 알람 등록")
    class CreateAlarmTest {

        @Test
        @DisplayName("성공: 알람 등록 요청이 성공하면 200을 반환한다")
        void success() throws Exception {

            // given
            AlarmFixture fixture = AlarmFixture.ALARM_03;
            AlarmRegisterRequest request = new AlarmRegisterRequest(
                new akuma.whiplash.domains.alarm.application.dto.request.PlaceRequest(
                    fixture.getAddress(),
                    fixture.getLatitude(),
                    fixture.getLongitude()
                ),
                fixture.getAlarmPurpose(),
                LocalTime.parse("08:30"),
                fixture.getRepeatDays().stream().map(Weekday::getDescription).toList(),
                fixture.getSoundType().getDescription()
            );
            setSecurityContext(buildContext(MEMBER_3));
            CreateAlarmResponse response = CreateAlarmResponse.builder().alarmId(123L).build();

            // when
            when(alarmUseCase.createAlarm(any(AlarmRegisterRequest.class), anyLong())).thenReturn(response);

            mockMvc.perform(post(BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            // then
            verify(alarmUseCase, times(1)).createAlarm(any(AlarmRegisterRequest.class), eq(MEMBER_3.getId()));
        }

        @Test
        @DisplayName("성공: 24시 형식의 시간을 보내면 0시로 변환되어 알람이 등록된다")
        void success_parse24HourTime() throws Exception {

            // given
            setSecurityContext(buildContext(MEMBER_3));
            String json = """
                {
                  "place": {
                    "address": "서울시 중구 퇴계로 24",
                    "latitude": 37.564213,
                    "longitude": 127.001698
                  },
                  "alarmPurpose": "도서관 정기 출석 알람",
                  "alarmTime": "24:30",
                  "repeatDays": ["월"],
                  "soundType": "알람 소리1"
                }
                """;
            CreateAlarmResponse response = CreateAlarmResponse.builder().alarmId(1L).build();

            // when
            when(alarmUseCase.createAlarm(any(AlarmRegisterRequest.class), anyLong())).thenReturn(response);

            mockMvc.perform(post(BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isOk());

            // then
            ArgumentCaptor<AlarmRegisterRequest> captor = ArgumentCaptor.forClass(AlarmRegisterRequest.class);
            verify(alarmUseCase, times(1)).createAlarm(captor.capture(), eq(MEMBER_3.getId()));
            assertThat(captor.getValue().alarmTime()).isEqualTo(LocalTime.of(0, 30));
        }

        @Test
        @DisplayName("실패: 회원이 존재하지 않으면 404를 반환한다")
        void fail_memberNotFound() throws Exception {

            // given
            AlarmFixture fixture = AlarmFixture.ALARM_04;
            AlarmRegisterRequest request = new AlarmRegisterRequest(
                new akuma.whiplash.domains.alarm.application.dto.request.PlaceRequest(
                    fixture.getAddress(),
                    fixture.getLatitude(),
                    fixture.getLongitude()
                ),
                fixture.getAlarmPurpose(),
                fixture.getTime(),
                fixture.getRepeatDays().stream().map(Weekday::getDescription).toList(),
                fixture.getSoundType().getDescription()
            );
            setSecurityContext(buildContext(MEMBER_4));

            // when
            when(alarmUseCase.createAlarm(any(AlarmRegisterRequest.class), anyLong()))
                .thenThrow(ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));

            // then
            mockMvc.perform(post(BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("실패: 같은 이름의 알람이 존재하면 409를 반환한다")
    void fail_duplicateAlarmPurpose() throws Exception {

        // given
        AlarmFixture fixture = AlarmFixture.ALARM_03;
        AlarmRegisterRequest request = new AlarmRegisterRequest(
            new akuma.whiplash.domains.alarm.application.dto.request.PlaceRequest(
                fixture.getAddress(),
                fixture.getLatitude(),
                fixture.getLongitude()
            ),
            fixture.getAlarmPurpose(),
            fixture.getTime(),
            fixture.getRepeatDays().stream().map(Weekday::getDescription).toList(),
            fixture.getSoundType().getDescription()
        );
        setSecurityContext(buildContext(MEMBER_3));

        // when & then
        when(alarmUseCase.createAlarm(any(AlarmRegisterRequest.class), anyLong()))
            .thenThrow(ApplicationException.from(AlarmErrorCode.DUPLICATE_ALARM_PURPOSE));

        mockMvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Nested
    @DisplayName("[POST] /api/v1/alarms/{id}/ring - 알람 울림")
    class RingAlarmTest {

        @Test
        @DisplayName("성공: 알람이 울리면 200을 반환한다")
        void success() throws Exception {

            // given
            setSecurityContext(buildContext(MEMBER_3));

            // when
            mockMvc.perform(post(BASE + "/{alarmId}/ring", 1L))
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
            mockMvc.perform(post(BASE + "/{alarmId}/ring", 1L))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("[POST] /api/v1/alarms/{alarmId}/checkin - 도착 인증")
    class CheckinTest {

        @Test
        @DisplayName("성공: 도착 인증 요청이 성공하면 200을 반환한다")
        void success() throws Exception {
            // given
            setSecurityContext(buildContext(MemberFixture.MEMBER_8));
            AlarmCheckinRequest request = buildCheckinRequest();

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/checkin", 1L)
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
            AlarmCheckinRequest request = buildCheckinRequest();

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/checkin", 1L)
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
            AlarmCheckinRequest request = buildCheckinRequest();

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/checkin", 1L)
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
            AlarmCheckinRequest request = buildCheckinRequest();

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/checkin", 1L)
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
            AlarmCheckinRequest request = buildCheckinRequest();

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/checkin", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 인증 가능 시간 전이면 400을 반환한다")
        void fail_notYetAvailable() throws Exception {
            // given
            setSecurityContext(buildContext(MemberFixture.MEMBER_13));
            doThrow(ApplicationException.from(AlarmErrorCode.CHECKIN_NOT_YET_AVAILABLE))
                .when(alarmUseCase)
                .checkinAlarm(anyLong(), anyLong(), any(AlarmCheckinRequest.class));
            AlarmCheckinRequest request = buildCheckinRequest();

            // when & then
            mockMvc.perform(post(BASE + "/{alarmId}/checkin", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("[DELETE] /api/v1/alarms/{alarmId} - 알람 삭제")
    class RemoveAlarmTest {

        @Test
        @DisplayName("성공: 알람 삭제 요청이 성공하면 200을 반환한다")
        void success() throws Exception {
            // given
            AlarmRemoveRequest request = new AlarmRemoveRequest("사유");
            setSecurityContext(buildContext(MEMBER_5));

            // when & then
            mockMvc.perform(delete(BASE + "/{alarmId}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            verify(alarmUseCase, times(1))
                .removeAlarm(eq(MEMBER_5.getId()), eq(1L), anyString());
        }

        @Test
        @DisplayName("실패: 알람이 존재하지 않으면 404를 반환한다")
        void fail_alarmNotFound() throws Exception {
            // given
            AlarmRemoveRequest request = new AlarmRemoveRequest("사유");
            setSecurityContext(buildContext(MEMBER_6));
            org.mockito.Mockito.doThrow(ApplicationException.from(akuma.whiplash.domains.alarm.exception.AlarmErrorCode.ALARM_NOT_FOUND))
                .when(alarmUseCase).removeAlarm(anyLong(), anyLong(), anyString());

            // when & then
            mockMvc.perform(delete(BASE + "/{alarmId}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패: 소유자가 아니면 403을 반환한다")
        void fail_permissionDenied() throws Exception {
            // given
            AlarmRemoveRequest request = new AlarmRemoveRequest("사유");
            setSecurityContext(buildContext(MEMBER_7));
            org.mockito.Mockito.doThrow(ApplicationException.from(AuthErrorCode.PERMISSION_DENIED))
                .when(alarmUseCase).removeAlarm(anyLong(), anyLong(), anyString());

            // when & then
            mockMvc.perform(delete(BASE + "/{alarmId}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패: 삭제 사유가 비어 있으면 400을 반환한다")
        void fail_reasonBlank() throws Exception {
            // given
            AlarmRemoveRequest request = new AlarmRemoveRequest("");
            setSecurityContext(buildContext(MEMBER_8));

            // when & then
            mockMvc.perform(delete(BASE + "/{alarmId}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("[GET] /api/v1/alarms - 알람 목록 조회")
    class GetAlarmsTest {

        @Test
        @DisplayName("성공: 200 OK와 result.alarms 래퍼로 알람 목록을 반환한다")
        void success() throws Exception {
            // given
            setSecurityContext(buildContext(MEMBER_3));
            AlarmPreviewDto dto = AlarmPreviewDto.builder()
                .alarmId(1L)
                .alarmPurpose("출근")
                .repeatDays(List.of("월"))
                .alarmTime("07:00")
                .address("서울")
                .status("활성화")
                .arrivalCheckEnabled(false)
                .nextOccurrence(AlarmPreviewDto.OccurrenceInfo.builder()
                    .occurrenceId(null)
                    .scheduledDate(LocalDate.now())
                    .dayOfWeek("월")
                    .build())
                .nextNextOccurrence(AlarmPreviewDto.OccurrenceInfo.builder()
                    .occurrenceId(null)
                    .scheduledDate(LocalDate.now().plusDays(7))
                    .dayOfWeek("월")
                    .build())
                .build();

            when(alarmUseCase.getAlarms(anyLong()))
                .thenReturn(GetAlarmsResponse.builder().alarms(List.of(dto)).build());

            // when & then
            mockMvc.perform(get(BASE))
                .andExpect(status().isOk());

            verify(alarmUseCase, times(1)).getAlarms(eq(MEMBER_3.getId()));
        }

        @Test
        @DisplayName("실패: 회원이 없으면 404와 에러 코드를 반환한다")
        void fail_memberNotFound() throws Exception {
            // given
            setSecurityContext(buildContext(MEMBER_4));

            when(alarmUseCase.getAlarms(anyLong()))
                .thenThrow(ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(get(BASE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(MemberErrorCode.MEMBER_NOT_FOUND.getCustomCode()));
        }
    }

    @Nested
    @DisplayName("[GET] /api/v1/alarms/sync - 알람 전체 동기화 조회")
    class GetSyncAlarmsTest {

        @Test
        @DisplayName("성공: 200 OK와 서버 시간 및 동기화 알람 목록을 반환한다")
        void success() throws Exception {
            // given
            setSecurityContext(buildContext(MEMBER_3));
            LocalDateTime scheduledAt = LocalDateTime.now().plusDays(1);
            AlarmSyncItemDto dto = AlarmSyncItemDto.builder()
                .alarmId(1L)
                .alarmRevision(2)
                .status("활성화")
                .nextOccurrence(AlarmSyncItemDto.NextOccurrenceInfo.builder()
                    .occurrenceId(10L)
                    .scheduledAt(scheduledAt)
                    .build())
                .build();
            when(alarmUseCase.getSyncAlarms(anyLong()))
                .thenReturn(AlarmSyncResponse.builder()
                    .serverTime(LocalDateTime.now())
                    .alarms(List.of(dto))
                    .build());

            // when
            mockMvc.perform(get(BASE + "/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.serverTime").exists())
                .andExpect(jsonPath("$.result.alarms[0].alarmId").value(1L))
                .andExpect(jsonPath("$.result.alarms[0].alarmRevision").value(2))
                .andExpect(jsonPath("$.result.alarms[0].status").value("활성화"))
                .andExpect(jsonPath("$.result.alarms[0].nextOccurrence.occurrenceId").value(10L));

            // then
            verify(alarmUseCase, times(1)).getSyncAlarms(eq(MEMBER_3.getId()));
        }

        @Test
        @DisplayName("실패: 회원이 없으면 404와 에러 코드를 반환한다")
        void fail_memberNotFound() throws Exception {
            // given
            setSecurityContext(buildContext(MEMBER_4));
            when(alarmUseCase.getSyncAlarms(anyLong()))
                .thenThrow(ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(get(BASE + "/sync"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(MemberErrorCode.MEMBER_NOT_FOUND.getCustomCode()));
        }
    }
}
