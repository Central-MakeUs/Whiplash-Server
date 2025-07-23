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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Getter
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
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    @Enumerated(EnumType.STRING)
    @Column(name = "deactivate_type", length = 20, nullable = false)
    private DeactivateType deactivateType;

    @Column(name = "deactivate_at")
    private LocalDateTime deactivateAt;

    @Column(name = "checkin_time")
    private LocalDateTime checkinTime;

    @Column(name = "alarm_ringing", nullable = false)
    private boolean alarmRinging;

    @Column(name = "ad_watched", nullable = false)
    private boolean adWatched;

    @Column(name = "dismissed_at")
    private LocalDateTime dismissedAt;

    @Column(name = "ringing_count", nullable = false)
    private int ringingCount;
}
