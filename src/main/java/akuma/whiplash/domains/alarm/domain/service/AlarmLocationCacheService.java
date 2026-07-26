package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.domain.constant.LocationSource;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.place.domain.client.GoogleClient;
import akuma.whiplash.domains.place.domain.model.PlaceDetailsCriteria;
import akuma.whiplash.domains.place.domain.model.SelectedPlaceDetail;
import akuma.whiplash.global.log.NoMethodLog;
import akuma.whiplash.global.util.date.TimeProvider;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@NoMethodLog
public class AlarmLocationCacheService {

    private static final int CACHE_VALID_DAYS = 29;

    private final GoogleClient googleClient;
    private final TimeProvider timeProvider;

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
    public void refreshGoogleLocationCache(AlarmEntity alarm) {
        refreshGoogleLocationCache(alarm, null);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void refreshGoogleLocationCache(AlarmEntity alarm, String sessionToken) {
        SelectedPlaceDetail detail = googleClient.getPlaceDetails(
            new PlaceDetailsCriteria(alarm.getGooglePlaceId(), sessionToken, null, null)
        );
        alarm.updateGooglePlaceLocation(
            detail.providerPlaceId(),
            detail.address(),
            detail.latitude(),
            detail.longitude(),
            timeProvider.now()
        );
    }
}
