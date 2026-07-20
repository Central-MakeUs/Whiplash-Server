package akuma.whiplash.domains.device.domain.service;

import static akuma.whiplash.domains.device.exception.DeviceErrorCode.DEVICE_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import akuma.whiplash.domains.device.application.dto.request.DeviceUpdateRequest;
import akuma.whiplash.domains.device.application.dto.response.DeviceUpdateResponse;
import akuma.whiplash.domains.member.domain.contants.MemberStatus;
import akuma.whiplash.domains.member.domain.contants.Role;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import akuma.whiplash.domains.member.persistence.entity.MemberDeviceEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.util.date.TimeProvider;
import akuma.whiplash.infrastructure.redis.RedisService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("DeviceCommandService Unit Test")
@ExtendWith(MockitoExtension.class)
class DeviceCommandServiceTest {

    @Mock private MemberDeviceRepository memberDeviceRepository;
    @Mock private RedisService redisService;
    @Mock private TimeProvider timeProvider;

    @InjectMocks
    private DeviceCommandServiceImpl deviceCommandService;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    private MemberEntity buildMember(Long memberId) {
        return MemberEntity.builder()
            .id(memberId)
            .provider(SocialType.GOOGLE)
            .providerUserId("provider-001")
            .email("user@test.com")
            .nickname("테스트유저")
            .role(Role.USER)
            .status(MemberStatus.ACTIVE)
            .build();
    }

    private MemberDeviceEntity buildDevice(MemberEntity member, String deviceId, String platform, String fcmToken) {
        return MemberDeviceEntity.builder()
            .member(member)
            .deviceId(deviceId)
            .platform(platform)
            .fcmToken(fcmToken)
            .isLoggedIn(true)
            .appVersion("1.0.0")
            .osVersion("14")
            .timeZone("Asia/Seoul")
            .build();
    }

    @Nested
    @DisplayName("modifyDevice - 기기 정보 갱신")
    class ModifyDeviceTest {

        @Test
        @DisplayName("성공: 기기 정보가 DB에 갱신되고 FCM 토큰이 Redis에 반영된다")
        void success() {
            // given
            Long memberId = 1L;
            String deviceId = "device-success";
            String newFcmToken = "new-fcm-token";
            DeviceUpdateRequest request = new DeviceUpdateRequest(
                deviceId,
                "IOS",
                newFcmToken,
                "2.0.0",
                "18",
                "America/New_York"
            );

            MemberDeviceEntity device = spy(
                buildDevice(buildMember(memberId), deviceId, "ANDROID", "old-fcm-token")
            );

            when(memberDeviceRepository.findByMember_IdAndDeviceId(memberId, deviceId))
                .thenReturn(Optional.of(device));
            when(timeProvider.now()).thenReturn(FIXED_NOW);

            // when
            DeviceUpdateResponse response = deviceCommandService.modifyDevice(memberId, request);

            // then
            verify(device).updateDevice(eq(newFcmToken), eq("IOS"), eq("2.0.0"), eq("18"), eq("America/New_York"), eq(FIXED_NOW));
            verify(redisService).upsertFcmToken(memberId, deviceId, newFcmToken);
            assertThat(response.deviceId()).isEqualTo(deviceId);
            assertThat(response.platform()).isEqualTo("IOS");
            assertThat(response.fcmToken()).isEqualTo(newFcmToken);
            assertThat(response.appVersion()).isEqualTo("2.0.0");
            assertThat(response.osVersion()).isEqualTo("18");
            assertThat(response.timeZone()).isEqualTo("America/New_York");
            assertThat(response.updatedAt()).isNotNull();
        }

        @Test
        @DisplayName("실패: 등록되지 않은 기기면 DEVICE_NOT_FOUND 예외를 던진다")
        void fail_deviceNotFound() {
            // given
            Long memberId = 1L;
            DeviceUpdateRequest request = new DeviceUpdateRequest(
                "unknown-device",
                "ANDROID",
                "new-fcm-token",
                "1.0.0",
                "14",
                "Asia/Seoul"
            );

            when(memberDeviceRepository.findByMember_IdAndDeviceId(memberId, "unknown-device"))
                .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deviceCommandService.modifyDevice(memberId, request))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                    assertThat(e.getCode()).isEqualTo(DEVICE_NOT_FOUND)
                );

            verify(redisService, never()).upsertFcmToken(any(), any(), any());
        }
    }
}
