package akuma.whiplash.domains.alarm.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("만료된 알람 위치 캐시를 삭제한다")
    class RemoveExpiredLocationCachesTest {

        @Test
        @DisplayName("성공: Google 좌표 캐시와 핀 주소 캐시를 각각 정리한다")
        void success() {
            // given
            LocalDateTime now = LocalDateTime.of(2026, 7, 25, 12, 0);
            given(timeProvider.now()).willReturn(now);
            given(alarmRepository.updateLocationCachesBySourceAndCachedAtBefore(
                LocationSource.GOOGLE_PLACE, now.minusDays(29)
            )).willReturn(2);
            given(alarmRepository.updateExpiredUserPinAddressCaches(
                LocationSource.USER_PIN, now.minusDays(29)
            )).willReturn(3);

            // when
            int cleared = alarmLocationCacheCleanupService.removeExpiredLocationCaches();

            // then
            assertThat(cleared).isEqualTo(5);
            verify(alarmRepository).updateLocationCachesBySourceAndCachedAtBefore(
                LocationSource.GOOGLE_PLACE, now.minusDays(29)
            );
            verify(alarmRepository).updateExpiredUserPinAddressCaches(
                LocationSource.USER_PIN, now.minusDays(29)
            );
        }
    }
}
