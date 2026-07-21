package akuma.whiplash.domains.ad.persistence.entity;

import akuma.whiplash.domains.ad.application.dto.etc.AdMobRewardCallback;
import akuma.whiplash.domains.ad.domain.constant.AdPurpose;
import akuma.whiplash.domains.ad.domain.constant.AdSessionStatus;
import akuma.whiplash.domains.ad.exception.AdErrorCode;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.global.entity.BaseTimeEntity;
import akuma.whiplash.global.exception.ApplicationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Getter
@SuperBuilder
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ad_session")
public class AdSessionEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ad_session_id", length = 64, nullable = false, unique = true)
    private String adSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alarm_id", nullable = false)
    private AlarmEntity alarm;

    @Column(name = "device_id", length = 255, nullable = false)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 50, nullable = false)
    private AdPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private AdSessionStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "transaction_id", length = 128, unique = true)
    private String transactionId;

    @Column(name = "ad_unit_id", length = 255)
    private String adUnitId;

    @Column(name = "reward_amount")
    private Integer rewardAmount;

    @Column(name = "reward_item", length = 100)
    private String rewardItem;

    @Column(name = "raw_callback_received_at")
    private LocalDateTime rawCallbackReceivedAt;

    public void verify(AdMobRewardCallback callback, LocalDateTime now) {
        if (status == AdSessionStatus.CONSUMED) {
            throw ApplicationException.from(AdErrorCode.AD_SESSION_ALREADY_CONSUMED);
        }
        if (now.isAfter(expiresAt)) {
            this.status = AdSessionStatus.EXPIRED;
            throw ApplicationException.from(AdErrorCode.AD_SESSION_EXPIRED);
        }
        if (status != AdSessionStatus.ISSUED) {
            throw ApplicationException.from(AdErrorCode.AD_SESSION_NOT_VERIFIED);
        }

        this.status = AdSessionStatus.VERIFIED;
        this.verifiedAt = now;
        this.transactionId = callback.transactionId();
        this.adUnitId = callback.adUnitId();
        this.rewardAmount = callback.rewardAmount();
        this.rewardItem = callback.rewardItem();
        this.rawCallbackReceivedAt = now;
    }

    public void validateForConsume(Long memberId, Long alarmId, String deviceId, AdPurpose purpose, LocalDateTime now) {
        if (status == AdSessionStatus.CONSUMED) {
            throw ApplicationException.from(AdErrorCode.AD_SESSION_ALREADY_CONSUMED);
        }
        if (now.isAfter(expiresAt)) {
            this.status = AdSessionStatus.EXPIRED;
            throw ApplicationException.from(AdErrorCode.AD_SESSION_EXPIRED);
        }
        if (status != AdSessionStatus.VERIFIED) {
            throw ApplicationException.from(AdErrorCode.AD_SESSION_NOT_VERIFIED);
        }
        if (!this.member.getId().equals(memberId)
            || !this.alarm.getId().equals(alarmId)
            || !this.deviceId.equals(deviceId)
            || this.purpose != purpose) {
            throw ApplicationException.from(AdErrorCode.AD_SESSION_MISMATCH);
        }
    }

    public void consume(LocalDateTime now) {
        this.status = AdSessionStatus.CONSUMED;
        this.consumedAt = now;
    }
}
