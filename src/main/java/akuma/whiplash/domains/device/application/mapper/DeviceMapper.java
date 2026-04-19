package akuma.whiplash.domains.device.application.mapper;

import akuma.whiplash.domains.device.application.dto.response.FcmTokenUpdateResponse;
import akuma.whiplash.domains.member.persistence.entity.MemberDeviceEntity;

public class DeviceMapper {

    private DeviceMapper() { throw new IllegalArgumentException(); }

    public static FcmTokenUpdateResponse mapToFcmTokenUpdateResponse(MemberDeviceEntity device) {
        return FcmTokenUpdateResponse.builder()
            .deviceId(device.getDeviceId())
            .fcmToken(device.getFcmToken())
            .updatedAt(device.getLastActiveAt())
            .build();
    }
}
