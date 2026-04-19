package akuma.whiplash.domains.device.domain.service;

import static akuma.whiplash.domains.device.exception.DeviceErrorCode.DEVICE_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import akuma.whiplash.domains.device.application.dto.request.FcmTokenUpdateRequest;
import akuma.whiplash.domains.device.application.dto.response.FcmTokenUpdateResponse;
import akuma.whiplash.domains.member.domain.contants.MemberStatus;
import akuma.whiplash.domains.member.domain.contants.Role;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import akuma.whiplash.domains.member.persistence.entity.MemberDeviceEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberDeviceRepository;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.infrastructure.redis.RedisService;
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

    @InjectMocks
    private DeviceCommandServiceImpl deviceCommandService;

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
            .build();
    }

    @Nested
    @DisplayName("modifyFcmToken - FCM 토큰 갱신")
    class ModifyFcmTokenTest {

        @Test
        @DisplayName("성공: FCM 토큰이 DB와 Redis에 갱신되고 응답을 반환한다")
        void success() {
            // given
            Long memberId = 1L;
            String deviceId = "device-success";
            String newFcmToken = "new-fcm-token";
            FcmTokenUpdateRequest request = new FcmTokenUpdateRequest(deviceId, newFcmToken);

            MemberDeviceEntity device = spy(
                buildDevice(buildMember(memberId), deviceId, "ANDROID", "old-fcm-token")
            );

            when(memberDeviceRepository.findByMember_IdAndDeviceId(memberId, deviceId))
                .thenReturn(Optional.of(device));

            // when
            FcmTokenUpdateResponse response = deviceCommandService.modifyFcmToken(memberId, request);

            // then
            verify(device).updateFcmToken(newFcmToken);
            verify(redisService).upsertFcmToken(memberId, deviceId, newFcmToken);
            assertThat(response.deviceId()).isEqualTo(deviceId);
            assertThat(response.fcmToken()).isEqualTo(newFcmToken);
            assertThat(response.updatedAt()).isNotNull();
        }

        @Test
        @DisplayName("실패: 등록되지 않은 기기면 DEVICE_NOT_FOUND 예외를 던진다")
        void fail_deviceNotFound() {
            // given
            Long memberId = 1L;
            FcmTokenUpdateRequest request = new FcmTokenUpdateRequest("unknown-device", "new-fcm-token");

            when(memberDeviceRepository.findByMember_IdAndDeviceId(memberId, "unknown-device"))
                .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deviceCommandService.modifyFcmToken(memberId, request))
                .isInstanceOf(ApplicationException.class)
                .extracting("code")
                .isEqualTo(DEVICE_NOT_FOUND);

            verify(redisService, never()).upsertFcmToken(any(), any(), any());
        }

        @Test
        @DisplayName("성공: FCM 토큰만 갱신되고 platform 등 다른 필드는 변경되지 않는다")
        void success_doesNotOverwriteOtherFields() {
            // given
            Long memberId = 1L;
            String deviceId = "device-ios";
            FcmTokenUpdateRequest request = new FcmTokenUpdateRequest(deviceId, "updated-fcm");

            MemberDeviceEntity device = spy(
                buildDevice(buildMember(memberId), deviceId, "IOS", "old-ios-fcm")
            );

            when(memberDeviceRepository.findByMember_IdAndDeviceId(memberId, deviceId))
                .thenReturn(Optional.of(device));

            // when
            deviceCommandService.modifyFcmToken(memberId, request);

            // then: updateFcmToken만 호출되고, updateOnLogin(platform 덮어쓰기)은 호출되지 않는다
            verify(device).updateFcmToken("updated-fcm");
            verify(device, never()).updateOnLogin(any(), any(), any(), any());
        }
    }
}
