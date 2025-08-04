package akuma.whiplash.domains.alarm.persistence.entity;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.ALREADY_DEACTIVATED;

import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
@Table(name = "alarm_occurrence")
public class AlarmOccurrenceEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alarm_id", nullable = false)
    private AlarmEntity alarm;

    @Column(nullable = false)
    private LocalDate date; // 알람이 원래 울려야 했던 날짜

    @Column(nullable = false)
    private LocalTime time; // 알람이 원래 울려야 했던 시간

    @Enumerated(EnumType.STRING)
    @Column(name = "deactivate_type", length = 20, nullable = false)
    private DeactivateType deactivateType;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "checkin_time")
    private LocalDateTime checkinTime;

    @Column(name = "alarm_ringing", nullable = false)
    private boolean alarmRinging;

    @Column(name = "ringing_count", nullable = false)
    private int ringingCount;

    @Column(name = "member_active_status",nullable = false)
    private boolean memberActiveStatus;

    public void deactivate(DeactivateType type, LocalDateTime time) {
        this.deactivateType = type;        // 알람 종료 방식 설정: OFF 또는 CHECKIN
        this.deactivatedAt = time;         // 알람을 끈 시간
    }

    public void checkin(LocalDateTime now) {
        this.deactivateType = DeactivateType.CHECKIN;
        this.checkinTime = now;
    }
}
