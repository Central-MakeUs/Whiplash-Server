package akuma.whiplash.domains.member.persistence.entity;

import akuma.whiplash.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "member_device",
    uniqueConstraints = @UniqueConstraint(
        name = "UK_MEMBER_DEVICE",
        columnNames = {"member_id", "device_id"}
    )
)
public class MemberDeviceEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(nullable = false, length = 20)
    private String platform;

    @Column(name = "fcm_token", length = 255)
    private String fcmToken;

    @Column(name = "is_logged_in", nullable = false)
    private boolean isLoggedIn;

    @Column(name = "app_version", length = 30)
    private String appVersion;

    @Column(name = "os_version", length = 30)
    private String osVersion;

    @Builder.Default
    @Column(name = "time_zone", nullable = false, length = 50, columnDefinition = "VARCHAR(50) DEFAULT 'Asia/Seoul'")
    private String timeZone = "Asia/Seoul";

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    public void updateDevice(
        String fcmToken,
        String platform,
        String appVersion,
        String osVersion,
        String timeZone,
        LocalDateTime now
    ) {
        this.fcmToken = fcmToken;
        this.platform = platform;
        this.appVersion = appVersion;
        this.osVersion = osVersion;
        this.timeZone = timeZone;
        this.isLoggedIn = true;
        this.lastActiveAt = now;
    }

    public void logout() {
        this.isLoggedIn = false;
    }
}
