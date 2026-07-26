package akuma.whiplash.domains.alarm.persistence.entity;

import akuma.whiplash.domains.alarm.domain.constant.AlarmStatus;
import akuma.whiplash.domains.alarm.domain.constant.LocationSource;
import akuma.whiplash.domains.alarm.domain.constant.SoundType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.domain.util.RepeatDaysConverter;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.time.LocalTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Getter
@SuperBuilder
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "alarm")
public class AlarmEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Column(name = "alarm_purpose", length = 50, nullable = false)
    private String alarmPurpose;

    @Column(name = "alarm_time", nullable = false)
    private LocalTime time;

    @Convert(converter = RepeatDaysConverter.class)
    @Column(name = "repeat_days", columnDefinition = "TEXT", nullable = false)
    private List<Weekday> repeatDays;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "sound_type", length = 20, nullable = false)
    private SoundType soundType = SoundType.VIBRATION_ONLY;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 255)
    private String address;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "location_source", length = 20, nullable = false)
    private LocationSource locationSource = LocationSource.GOOGLE_PLACE;

    @Column(name = "google_place_id", length = 255)
    private String googlePlaceId;

    @Column(name = "location_cached_at")
    private LocalDateTime locationCachedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AlarmStatus status = AlarmStatus.ACTIVE;

    @Column(name = "next_scheduled_time")
    private LocalDateTime nextScheduledTime;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void updateNextScheduledTime(LocalDateTime nextScheduledTime) {
        this.nextScheduledTime = nextScheduledTime;
    }

    public void softDelete(LocalDateTime now) {
        this.status = AlarmStatus.DELETED;
        this.deletedAt = now;
    }

    public void updateGooglePlaceLocation(
        String googlePlaceId,
        String address,
        double latitude,
        double longitude,
        LocalDateTime locationCachedAt
    ) {
        this.locationSource = LocationSource.GOOGLE_PLACE;
        this.googlePlaceId = googlePlaceId;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationCachedAt = locationCachedAt;
    }

    public void clearGooglePlaceLocationCache() {
        if (locationSource != LocationSource.GOOGLE_PLACE) {
            return;
        }
        this.latitude = null;
        this.longitude = null;
        this.address = null;
        this.locationCachedAt = null;
    }

    public boolean hasGooglePlaceId() {
        return googlePlaceId != null && !googlePlaceId.isBlank();
    }
}
