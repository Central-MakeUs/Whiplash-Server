package akuma.whiplash.common.fixture;

import akuma.whiplash.domains.member.persistence.entity.MemberDeviceEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import lombok.Getter;

@Getter
public enum MemberDeviceFixture {

    ANDROID("device-uuid", "ANDROID", "fcm-token"),
    IOS("ios-device-uuid", "IOS", "ios-fcm-token");

    private final String deviceId;
    private final String platform;
    private final String fcmToken;

    MemberDeviceFixture(String deviceId, String platform, String fcmToken) {
        this.deviceId = deviceId;
        this.platform = platform;
        this.fcmToken = fcmToken;
    }

    public MemberDeviceEntity toEntity(MemberEntity member) {
        return MemberDeviceEntity.builder()
            .member(member)
            .deviceId(deviceId)
            .platform(platform)
            .fcmToken(fcmToken)
            .isLoggedIn(true)
            .appVersion("1.0.0")
            .osVersion("17")
            .timeZone("Asia/Seoul")
            .build();
    }
}
