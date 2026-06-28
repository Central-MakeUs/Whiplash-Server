package akuma.whiplash.domains.device.application.mapper;

import akuma.whiplash.domains.device.application.dto.response.DeviceUpdateResponse;
import akuma.whiplash.domains.member.persistence.entity.MemberDeviceEntity;

public class DeviceMapper {

    private DeviceMapper() { throw new IllegalArgumentException(); }

    public static DeviceUpdateResponse mapToDeviceUpdateResponse(MemberDeviceEntity device) {
        return DeviceUpdateResponse.builder()
            .deviceId(device.getDeviceId())
            .platform(device.getPlatform())
            .fcmToken(device.getFcmToken())
            .appVersion(device.getAppVersion())
            .osVersion(device.getOsVersion())
            .timeZone(device.getTimeZone())
            .updatedAt(device.getLastActiveAt())
            .build();
    }
}
