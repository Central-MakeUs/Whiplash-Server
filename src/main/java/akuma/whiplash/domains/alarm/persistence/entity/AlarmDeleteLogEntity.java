package akuma.whiplash.domains.alarm.persistence.entity;

import akuma.whiplash.domains.alarm.domain.constant.DeleteType;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
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
@Table(name = "alarm_delete_log")
public class AlarmDeleteLogEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alarm_id", nullable = false)
    private AlarmEntity alarm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Enumerated(EnumType.STRING)
    @Column(name = "delete_type", length = 20, nullable = false)
    private DeleteType deleteType;

    @Column(name = "reason", length = 2000, nullable = false)
    private String reason;

    @Column(name = "payment_id", length = 100)
    private String paymentId;

    @Column(name = "ad_proof_token", length = 255)
    private String adProofToken;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
