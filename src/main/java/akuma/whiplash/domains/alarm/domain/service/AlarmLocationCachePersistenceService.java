package akuma.whiplash.domains.alarm.domain.service;

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
    public void removeGoogleLocationCache(Long alarmId) {
        alarmRepository.findById(alarmId).ifPresent(AlarmEntity::clearGooglePlaceLocationCache);
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
