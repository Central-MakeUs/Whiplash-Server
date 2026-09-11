package akuma.whiplash.domains.alarm.domain.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import akuma.whiplash.domains.alarm.domain.constant.LocationSource;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.global.util.date.TimeProvider;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlarmLocationCacheCleanupService Unit Test")
class AlarmLocationCacheCleanupServiceTest {

    @Mock
    private AlarmRepository alarmRepository;
    @Mock
    private TimeProvider timeProvider;
    @InjectMocks
    private AlarmLocationCacheCleanupService alarmLocationCacheCleanupService;

    @Nested
    @DisplayName("만료된 Google 장소 위치 캐시를 삭제한다")
    class RemoveExpiredGoogleLocationCachesTest {

        @Test
        @DisplayName("성공: 29일 지난 Google 장소 캐시만 삭제한다")
        void success() {
            // given
            LocalDateTime now = LocalDateTime.of(2026, 7, 25, 12, 0);
            given(timeProvider.now()).willReturn(now);

            // when
            alarmLocationCacheCleanupService.removeExpiredGoogleLocationCaches();

            // then
            verify(alarmRepository).updateLocationCachesBySourceAndCachedAtBefore(
                LocationSource.GOOGLE_PLACE, now.minusDays(29)
            );
        }
    }
}
