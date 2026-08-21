package akuma.whiplash.loadtest.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.global.config.security.SecurityConfig;
import akuma.whiplash.global.config.security.jwt.JwtAuthenticationFilter;
import akuma.whiplash.loadtest.application.usecase.AlarmPipelineLoadTestUseCase;
import akuma.whiplash.loadtest.application.usecase.FcmBulkSendLoadTestUseCase;
import akuma.whiplash.loadtest.application.usecase.FcmTokenLoadTestUseCase;
import akuma.whiplash.loadtest.application.usecase.TestPushUseCase;
import akuma.whiplash.loadtest.application.dto.request.TestPushRequest;
import akuma.whiplash.loadtest.application.dto.response.TestPushResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("local")
@DisplayName("TestController Slice Test")
@WebMvcTest(
    controllers = TestController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        SecurityConfig.class, JwtAuthenticationFilter.class
    })
)
@AutoConfigureMockMvc(addFilters = false)
class TestControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private TestPushUseCase testPushUseCase;
    @MockitoBean private FcmBulkSendLoadTestUseCase fcmBulkUseCase;
    @MockitoBean private AlarmPipelineLoadTestUseCase alarmPipelineUseCase;
    @MockitoBean private FcmTokenLoadTestUseCase fcmTokenUseCase;

    @Nested
    @DisplayName("[POST] /api/v1/test/fcm/push - FCM 테스트 푸시 전송")
    class SendTestPushTest {

        @Test
        @DisplayName("성공: FCM 전송 결과를 반환한다")
        void success() throws Exception {
            // given
            TestPushRequest request = new TestPushRequest("direct-fcm-token", "테스트", "도착 확인", "nuntteo://main");
            when(testPushUseCase.createTestPush(any(TestPushRequest.class)))
                .thenReturn(new TestPushResponse(1, 0));

            // when
            // then
            mockMvc.perform(post("/api/v1/test/fcm/push")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.successCount").value(1))
                .andExpect(jsonPath("$.result.failedCount").value(0));
        }

        @Test
        @DisplayName("실패: title이 공백이면 400을 반환한다")
        void fail_titleBlank() throws Exception {
            // given
            TestPushRequest request = new TestPushRequest("direct-fcm-token", "", "도착 확인", "nuntteo://main");

            // when
            // then
            mockMvc.perform(post("/api/v1/test/fcm/push")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

}
