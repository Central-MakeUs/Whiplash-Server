package akuma.whiplash.domains.alarm.persistence.entity;

import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.domain.constant.DeactivationResult;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.global.entity.BaseTimeEntity;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
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
@Access(AccessType.FIELD)
@Table(name = "alarm_deactivation_log")
public class AlarmDeactivationLogEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alarm_occurrence_id", nullable = false)
    private AlarmOccurrenceEntity alarmOccurrence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Column(name = "payment_id", length = 512)
    private String paymentId;

    @Column(name = "ad_proof_token", length = 64)
    private String adProofToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "deactivate_type", length = 20, nullable = false)
    private DeactivateType deactivateType;

    @Column(name = "request_device_id", length = 100, nullable = false)
    private String requestDeviceId;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 20, nullable = false)
    private DeactivationResult result;

    @Column(name = "fail_reason", length = 500, nullable = false)
    private String failReason;
}
