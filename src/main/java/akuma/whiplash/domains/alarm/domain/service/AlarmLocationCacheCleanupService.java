package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.domain.constant.LocationSource;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.global.util.date.TimeProvider;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlarmLocationCacheCleanupService {

    private static final int CACHE_VALID_DAYS = 29;

    private final AlarmRepository alarmRepository;
    private final TimeProvider timeProvider;

    @Transactional
    public int clearExpiredGoogleLocationCaches() {
        LocalDateTime expiresAt = timeProvider.now().minusDays(CACHE_VALID_DAYS);
        return alarmRepository.clearLocationCachesBySourceAndCachedAtBefore(
            LocationSource.GOOGLE_PLACE,
            expiresAt
        );
    }
}
