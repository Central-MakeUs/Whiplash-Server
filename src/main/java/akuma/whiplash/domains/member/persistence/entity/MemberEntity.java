package akuma.whiplash.domains.member.persistence.entity;

import akuma.whiplash.domains.member.domain.contants.Role;
import akuma.whiplash.domains.member.domain.contants.SocialType;
import akuma.whiplash.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
@Table(name = "member")
public class MemberEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "social_id", unique = true, nullable = false)
    private String socialId;

    @Column(length = 50, nullable = false)
    private String email;

    @Column(length = 50, nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "privacy_policy", nullable = false)
    private boolean privacyPolicy;

    @Column(name = "push_notification_policy", nullable = false)
    private boolean pushNotificationPolicy;

    @Column(name = "privacy_agreed_at", nullable = false)
    private LocalDateTime privacyAgreedAt;

    @Column(name = "push_agreed_at", nullable = false)
    private LocalDateTime pushAgreedAt;

    public void updatePrivacyPolicy(boolean privacyPolicy) {
        this.privacyPolicy = privacyPolicy;
        this.privacyAgreedAt = LocalDateTime.now();
    }

    public void updatePushNotificationPolicy(boolean pushNotificationPolicy) {
        this.pushNotificationPolicy = pushNotificationPolicy;
        this.pushAgreedAt = LocalDateTime.now();
    }
}
