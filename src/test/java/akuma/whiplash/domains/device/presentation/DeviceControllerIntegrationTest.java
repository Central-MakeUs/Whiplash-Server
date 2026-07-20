package akuma.whiplash.domains.device.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import akuma.whiplash.common.config.IntegrationTest;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.auth.application.dto.etc.MemberContext;
import akuma.whiplash.domains.device.application.dto.request.DeviceUpdateRequest;
import akuma.whiplash.domains.device.exception.DeviceErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberDeviceEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.infrastructure.redis.RedisService;
import akuma.whiplash.global.config.security.jwt.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("DeviceController Integration Test")
@IntegrationTest
@AutoConfigureMockMvc
@Transactional(propagation = Propagation.NOT_SUPPORTED) // Redis 트랜잭션 이슈 해결을 위해 사용
class DeviceControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberDeviceRepository memberDeviceRepository;
    @Autowired private RedisService redisService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtProvider jwtProvider;

    private static final String BASE = "/api/v1/devices";

    @BeforeEach
    void setUp() {
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
        SecurityContextHolder.clearContext();
    }

    private void cleanup() {
        memberDeviceRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private String buildAccessToken(MemberEntity member, String deviceId) {
        return jwtProvider.generateAccessToken(member.getId(), member.getRole(), deviceId);
    }

    private void setSecurityContext(MemberEntity member, String deviceId) {
        MemberContext context = MemberContext.builder()
            .memberId(member.getId())
            .provider(member.getProvider())
            .email(member.getEmail())
            .nickname(member.getNickname())
            .role(member.getRole())
            .deviceId(deviceId)
            .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            context, null, List.of(new SimpleGrantedAuthority(context.role().name())));
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        SecurityContextHolder.setContext(sc);
    }

    @Nested
    @DisplayName("[PUT] /api/v1/devices/me - 기기 정보 갱신")
    class ModifyDeviceTest {

        @Test
        @DisplayName("성공: 기기 정보가 DB에 갱신되고 FCM 토큰이 Redis에 반영된다")
        void success() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_1.toEntity());
            String deviceId = "device-integration-update";
            String oldFcmToken = "old-fcm-token";
            String newFcmToken = "new-fcm-token";

            memberDeviceRepository.save(MemberDeviceEntity.builder()
                .member(member)
                .deviceId(deviceId)
                .platform("ANDROID")
                .fcmToken(oldFcmToken)
                .isLoggedIn(true)
                .appVersion("1.0.0")
                .osVersion("14")
                .timeZone("Asia/Seoul")
                .build());
            redisService.upsertFcmToken(member.getId(), deviceId, oldFcmToken);
            setSecurityContext(member, deviceId);

            DeviceUpdateRequest request = new DeviceUpdateRequest(
                deviceId,
                "IOS",
                newFcmToken,
                "2.0.0",
                "18",
                "America/New_York"
            );

            // when
            mockMvc.perform(put(BASE + "/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + buildAccessToken(member, deviceId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.deviceId").value(deviceId))
                .andExpect(jsonPath("$.result.platform").value("IOS"))
                .andExpect(jsonPath("$.result.fcmToken").value(newFcmToken))
                .andExpect(jsonPath("$.result.appVersion").value("2.0.0"))
                .andExpect(jsonPath("$.result.osVersion").value("18"))
                .andExpect(jsonPath("$.result.timeZone").value("America/New_York"))
                .andExpect(jsonPath("$.result.updatedAt").isNotEmpty());

            // then
            MemberDeviceEntity updated = memberDeviceRepository
                .findByMember_IdAndDeviceId(member.getId(), deviceId)
                .orElseThrow();
            assertThat(updated.getFcmToken()).isEqualTo(newFcmToken);
            assertThat(updated.getPlatform()).isEqualTo("IOS");
            assertThat(updated.getAppVersion()).isEqualTo("2.0.0");
            assertThat(updated.getOsVersion()).isEqualTo("18");
            assertThat(updated.getTimeZone()).isEqualTo("America/New_York");
            assertThat(redisService.getFcmTokenByDevice(deviceId)).isEqualTo(newFcmToken);
        }

        @Test
        @DisplayName("실패: 등록되지 않은 기기면 404와 DEVICE_NOT_FOUND 코드를 반환한다")
        void fail_deviceNotFound() throws Exception {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_2.toEntity());
            String unknownDeviceId = "device-not-registered";
            setSecurityContext(member, unknownDeviceId);

            DeviceUpdateRequest request = new DeviceUpdateRequest(
                unknownDeviceId,
                "ANDROID",
                "any-fcm-token",
                "1.0.0",
                "14",
                "Asia/Seoul"
            );

            // when & then
            mockMvc.perform(put(BASE + "/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + buildAccessToken(member, unknownDeviceId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(DeviceErrorCode.DEVICE_NOT_FOUND.getCustomCode()));
        }

        @Test
        @DisplayName("실패: 인증 정보가 없으면 401을 반환한다")
        void fail_unauthenticated() throws Exception {
            // given
            DeviceUpdateRequest request = new DeviceUpdateRequest(
                "device-id",
                "ANDROID",
                "fcm-token",
                "1.0.0",
                "14",
                "Asia/Seoul"
            );

            // when & then
            mockMvc.perform(put(BASE + "/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }
    }
}
