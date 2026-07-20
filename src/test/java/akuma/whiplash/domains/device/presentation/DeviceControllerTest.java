package akuma.whiplash.domains.device.presentation;

import static akuma.whiplash.common.fixture.MemberFixture.MEMBER_1;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.device.application.dto.request.DeviceUpdateRequest;
import akuma.whiplash.domains.device.application.dto.response.DeviceUpdateResponse;
import akuma.whiplash.domains.device.application.usecase.DeviceUseCase;
import akuma.whiplash.domains.device.exception.DeviceErrorCode;
import akuma.whiplash.global.config.security.SecurityConfig;
import akuma.whiplash.global.config.security.jwt.JwtAuthenticationFilter;
import akuma.whiplash.global.exception.ApplicationException;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("DeviceController Slice Test")
@WebMvcTest(
    controllers = DeviceController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            SecurityConfig.class,
            JwtAuthenticationFilter.class
        })
    }
)
@AutoConfigureMockMvc(addFilters = false)
class DeviceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private DeviceUseCase deviceUseCase;

    private static final String BASE = "/api/v1/devices";

    private MemberContext buildContext() {
        return MemberContext.builder()
            .memberId(MEMBER_1.getId())
            .role(MEMBER_1.getRole())
            .provider(MEMBER_1.getProvider())
            .email(MEMBER_1.getEmail())
            .nickname(MEMBER_1.getNickname())
            .deviceId("device-slice")
            .build();
    }

    private void setSecurityContext(MemberContext context) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            context, null, List.of(new SimpleGrantedAuthority(context.role().name())));
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        SecurityContextHolder.setContext(sc);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("[PUT] /api/v1/devices/me - 기기 정보 갱신")
    class ModifyDeviceTest {

        @Test
        @DisplayName("성공: 200과 갱신된 기기 정보 필드를 반환한다")
        void success() throws Exception {
            // given
            setSecurityContext(buildContext());
            DeviceUpdateRequest request = new DeviceUpdateRequest(
                "device-slice",
                "ANDROID",
                "new-fcm-token",
                "1.1.0",
                "15",
                "Asia/Seoul"
            );
            DeviceUpdateResponse response = DeviceUpdateResponse.builder()
                .deviceId("device-slice")
                .platform("ANDROID")
                .fcmToken("new-fcm-token")
                .appVersion("1.1.0")
                .osVersion("15")
                .timeZone("Asia/Seoul")
                .updatedAt(LocalDateTime.of(2026, 4, 19, 12, 0, 0))
                .build();

            when(deviceUseCase.modifyDevice(any(MemberContext.class), any(DeviceUpdateRequest.class)))
                .thenReturn(response);

            // when & then
            mockMvc.perform(put(BASE + "/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.deviceId").value("device-slice"))
                .andExpect(jsonPath("$.result.platform").value("ANDROID"))
                .andExpect(jsonPath("$.result.fcmToken").value("new-fcm-token"))
                .andExpect(jsonPath("$.result.appVersion").value("1.1.0"))
                .andExpect(jsonPath("$.result.osVersion").value("15"))
                .andExpect(jsonPath("$.result.timeZone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.result.updatedAt").isNotEmpty());
        }

        @Test
        @DisplayName("실패: deviceId가 공백이면 400을 반환한다")
        void fail_deviceIdBlank() throws Exception {
            // given
            setSecurityContext(buildContext());
            DeviceUpdateRequest request = new DeviceUpdateRequest(
                "",
                "ANDROID",
                "new-fcm-token",
                "1.1.0",
                "15",
                "Asia/Seoul"
            );

            // when & then
            mockMvc.perform(put(BASE + "/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: platform이 공백이면 400을 반환한다")
        void fail_platformBlank() throws Exception {
            // given
            setSecurityContext(buildContext());
            DeviceUpdateRequest request = new DeviceUpdateRequest(
                "device-slice",
                "",
                "new-fcm-token",
                "1.1.0",
                "15",
                "Asia/Seoul"
            );

            // when & then
            mockMvc.perform(put(BASE + "/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: fcmToken이 공백이면 400을 반환한다")
        void fail_fcmTokenBlank() throws Exception {
            // given
            setSecurityContext(buildContext());
            DeviceUpdateRequest request = new DeviceUpdateRequest(
                "device-slice",
                "ANDROID",
                "",
                "1.1.0",
                "15",
                "Asia/Seoul"
            );

            // when & then
            mockMvc.perform(put(BASE + "/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: timeZone이 공백이면 400을 반환한다")
        void fail_timeZoneBlank() throws Exception {
            // given
            setSecurityContext(buildContext());
            DeviceUpdateRequest request = new DeviceUpdateRequest(
                "device-slice",
                "ANDROID",
                "new-fcm-token",
                "1.1.0",
                "15",
                ""
            );

            // when & then
            mockMvc.perform(put(BASE + "/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: timeZone이 IANA Zone ID가 아니면 400을 반환한다")
        void fail_invalidTimeZone() throws Exception {
            // given
            setSecurityContext(buildContext());
            DeviceUpdateRequest request = new DeviceUpdateRequest(
                "device-slice",
                "ANDROID",
                "new-fcm-token",
                "1.1.0",
                "15",
                "UTC+9"
            );

            // when & then
            mockMvc.perform(put(BASE + "/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 등록되지 않은 기기면 404를 반환한다")
        void fail_deviceNotFound() throws Exception {
            // given
            setSecurityContext(buildContext());
            DeviceUpdateRequest request = new DeviceUpdateRequest(
                "unknown-device",
                "ANDROID",
                "any-fcm",
                "1.1.0",
                "15",
                "Asia/Seoul"
            );

            when(deviceUseCase.modifyDevice(any(MemberContext.class), any(DeviceUpdateRequest.class)))
                .thenThrow(ApplicationException.from(DeviceErrorCode.DEVICE_NOT_FOUND));

            // when & then
            mockMvc.perform(put(BASE + "/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(DeviceErrorCode.DEVICE_NOT_FOUND.getCustomCode()));
        }
    }
}
