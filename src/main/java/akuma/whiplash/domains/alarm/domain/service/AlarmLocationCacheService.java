package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.domain.constant.LocationSource;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.place.domain.client.GoogleClient;
import akuma.whiplash.domains.place.domain.model.PlaceDetailsCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.SelectedPlaceDetail;
import akuma.whiplash.global.log.NoMethodLog;
import akuma.whiplash.global.util.date.TimeProvider;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@NoMethodLog
@Slf4j
public class AlarmLocationCacheService {

    private static final int CACHE_VALID_DAYS = 29;

    private final GoogleClient googleClient;
    private final TimeProvider timeProvider;
    private final AlarmRepository alarmRepository;
    private final AlarmLocationCachePersistenceService alarmLocationCachePersistenceService;

    public boolean hasValidGoogleLocationCache(AlarmEntity alarm, LocalDateTime now) {
        return alarm.getLocationSource() == LocationSource.GOOGLE_PLACE
            && alarm.getLatitude() != null
            && alarm.getLongitude() != null
            && alarm.getLocationCachedAt() != null
            && alarm.getLocationCachedAt().plusDays(CACHE_VALID_DAYS).isAfter(now);
    }

    public boolean canRefreshGoogleLocationCache(AlarmEntity alarm) {
        return alarm.getLocationSource() == LocationSource.GOOGLE_PLACE && alarm.hasGooglePlaceId();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String getAddressForAlarmList(AlarmEntity alarm) {
        LocalDateTime now = timeProvider.now();
        if (hasValidAddressCache(alarm, now)) {
            return alarm.getAddress();
        }

        try {
            if (canRefreshGoogleLocationCache(alarm)) {
                modifyGoogleLocationCache(alarm);
                return alarm.getAddress();
            }
            if (alarm.getLocationSource() == LocationSource.USER_PIN
                && alarm.getLatitude() != null
                && alarm.getLongitude() != null) {
                PlaceDetail detail = googleClient.reverseGeocode(alarm.getLatitude(), alarm.getLongitude(), "ko");
                LocalDateTime locationCachedAt = nextCacheTimestamp(alarm.getLocationCachedAt());
                alarmLocationCachePersistenceService.modifyUserPinAddressCache(
                    alarm.getId(), detail.address(), locationCachedAt
                );
                return detail.address();
            }
        } catch (RuntimeException exception) {
            if (alarm.getLocationSource() == LocationSource.USER_PIN) {
                try {
                    alarmLocationCachePersistenceService.removeUserPinAddressCache(
                        alarm.getId(), alarm.getLocationCachedAt()
                    );
                } catch (RuntimeException cleanupException) {
                    log.warn("알람 주소 캐시 정리에 실패했습니다. alarmId={}, cause={}",
                        alarm.getId(), cleanupException.getClass().getSimpleName());
                }
            }
        }
        return null;
    }

    private boolean hasValidAddressCache(AlarmEntity alarm, LocalDateTime now) {
        return alarm.getAddress() != null
            && !alarm.getAddress().isBlank()
            && alarm.getLocationCachedAt() != null
            && alarm.getLocationCachedAt().plusDays(CACHE_VALID_DAYS).isAfter(now);
    }

    private LocalDateTime nextCacheTimestamp(LocalDateTime previousCachedAt) {
        // DATETIME(6) 값을 캐시 버전으로 비교하므로 같은 시각의 재조회도 새 버전으로 기록한다.
        LocalDateTime now = timeProvider.now().truncatedTo(ChronoUnit.MICROS);
        if (previousCachedAt == null || now.isAfter(previousCachedAt)) {
            return now;
        }
        return previousCachedAt.truncatedTo(ChronoUnit.MICROS).plus(1, ChronoUnit.MICROS);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void modifyGoogleLocationCache(AlarmEntity alarm) {
        modifyGoogleLocationCache(alarm, null);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void modifyGoogleLocationCache(Long alarmId) {
        alarmRepository.findById(alarmId).ifPresent(this::modifyGoogleLocationCache);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void modifyGoogleLocationCache(AlarmEntity alarm, String sessionToken) {
        LocalDateTime expectedCachedAt = alarm.getLocationCachedAt();
        boolean hadValidCoordinates = hasValidGoogleLocationCache(alarm, timeProvider.now());
        try {
            SelectedPlaceDetail detail = googleClient.getPlaceDetails(
                new PlaceDetailsCriteria(alarm.getGooglePlaceId(), sessionToken, null, null)
            );
            LocalDateTime locationCachedAt = nextCacheTimestamp(expectedCachedAt);
            alarm.updateGooglePlaceLocation(
                detail.providerPlaceId(),
                detail.address(),
                detail.latitude(),
                detail.longitude(),
                locationCachedAt
            );
            alarmLocationCachePersistenceService.modifyGoogleLocationCache(alarm.getId(), detail, locationCachedAt);
        } catch (RuntimeException exception) {
            if (!hadValidCoordinates) {
                alarm.clearGooglePlaceLocationCache();
                alarmLocationCachePersistenceService.removeGoogleLocationCache(alarm.getId(), expectedCachedAt);
            }
            throw exception;
        }
    }
}
