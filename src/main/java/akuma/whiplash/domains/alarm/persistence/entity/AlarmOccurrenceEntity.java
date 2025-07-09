package akuma.whiplash.domains.alarm.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
public class AlarmOccurrenceEntity {

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

    @Column(name = "checked_in", nullable = false)
    private Boolean checkedIn;

    @Column(name = "checkin_time")
    private LocalDateTime checkinTime;

    @Column(name = "alarm_ringing", nullable = false)
    private Boolean alarmRinging;

    @Column(name = "ad_watched", nullable = false)
    private Boolean adWatched;

    @Column(name = "dismissed_at")
    private LocalDateTime dismissedAt;

    @Column(name = "ringing_count", nullable = false)
    private int ringingCount;
}
