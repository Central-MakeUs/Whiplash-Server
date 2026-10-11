package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.domain.constant.LocationSource;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.place.domain.model.SelectedPlaceDetail;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlarmLocationCachePersistenceService {

    private final AlarmRepository alarmRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void modifyGoogleLocationCache(
        Long alarmId,
        SelectedPlaceDetail detail,
        LocalDateTime locationCachedAt
    ) {
        alarmRepository.findById(alarmId).ifPresent(alarm -> updateGoogleLocationCache(alarm, detail, locationCachedAt));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeGoogleLocationCache(Long alarmId, LocalDateTime expectedCachedAt) {
        alarmRepository.updateGoogleLocationCacheToEmptyIfUnchanged(
            alarmId, LocationSource.GOOGLE_PLACE, expectedCachedAt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void modifyUserPinAddressCache(Long alarmId, String address, LocalDateTime locationCachedAt) {
        alarmRepository.findById(alarmId)
            .ifPresent(alarm -> alarm.updateUserPinAddressCache(address, locationCachedAt));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeUserPinAddressCache(Long alarmId, LocalDateTime expectedCachedAt) {
        alarmRepository.updateUserPinAddressCacheToEmptyIfUnchanged(
            alarmId, LocationSource.USER_PIN, expectedCachedAt
        );
    }

    private void updateGoogleLocationCache(
        AlarmEntity alarm,
        SelectedPlaceDetail detail,
        LocalDateTime locationCachedAt
    ) {
        alarm.updateGooglePlaceLocation(
            detail.providerPlaceId(),
            detail.address(),
            detail.latitude(),
            detail.longitude(),
            locationCachedAt
        );
    }
}
