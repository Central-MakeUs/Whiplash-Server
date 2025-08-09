package akuma.whiplash.domains.alarm.persistence.entity;


import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
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
import jakarta.persistence.UniqueConstraint;
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
@Table(
    name = "alarm_occurrence",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_alarm_date",
            columnNames = {"alarm_id", "date"}
        )
    }
)
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

    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent;

    public void deactivate(DeactivateType type, LocalDateTime time) {
        this.deactivateType = type;        // 알람 종료 방식 설정: OFF 또는 CHECKIN
        this.deactivatedAt = time;         // 알람을 끈 시간
    }

    public void checkin(LocalDateTime now) {
        this.deactivateType = DeactivateType.CHECKIN;
        this.checkinTime = now;
    }

    public void updateReminderSent(boolean reminderSent) {
        this.reminderSent = reminderSent;
    }
}
