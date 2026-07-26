package akuma.whiplash.domains.alarm.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.domains.alarm.domain.constant.LocationSource;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.place.domain.client.GoogleClient;
import akuma.whiplash.domains.place.domain.model.PlaceDetailsCriteria;
import akuma.whiplash.domains.place.domain.model.SelectedPlaceDetail;
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
@DisplayName("AlarmLocationCacheService Unit Test")
class AlarmLocationCacheServiceTest {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 7, 25, 12, 0);

    @Mock
    private GoogleClient googleClient;
    @Mock
    private TimeProvider timeProvider;
    @InjectMocks
    private AlarmLocationCacheService alarmLocationCacheService;

    @Nested
    @DisplayName("hasValidGoogleLocationCache - 캐시 유효성 판단")
    class HasValidGoogleLocationCacheTest {

        @Test
        @DisplayName("성공: 저장 후 29일 전의 Google 장소 캐시는 유효하다")
        void success() {
            // given
            AlarmEntity alarm = googlePlaceAlarm(FIXED_NOW.minusDays(28).plusSeconds(1));

            // when
            boolean result = alarmLocationCacheService.hasValidGoogleLocationCache(alarm, FIXED_NOW);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("성공: 저장 후 29일이 지난 Google 장소 캐시는 만료된다")
        void success_expiredAfterTwentyNineDays() {
            // given
            AlarmEntity alarm = googlePlaceAlarm(FIXED_NOW.minusDays(29));

            // when
            boolean result = alarmLocationCacheService.hasValidGoogleLocationCache(alarm, FIXED_NOW);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("refreshGoogleLocationCache - Google 장소 캐시 갱신")
    class RefreshGoogleLocationCacheTest {

        @Test
        @DisplayName("성공: Place ID로 조회한 좌표와 주소를 새 캐시로 저장한다")
        void success() {
            // given
            AlarmEntity alarm = googlePlaceAlarm(FIXED_NOW.minusDays(29));
            SelectedPlaceDetail detail = new SelectedPlaceDetail(
                "서울특별시 중구 세종대로 110", 37.5663, 126.9779, "KR", "google-place-id"
            );
            given(timeProvider.now()).willReturn(FIXED_NOW);
            given(googleClient.getPlaceDetails(new PlaceDetailsCriteria("google-place-id", null, null, null)))
                .willReturn(detail);

            // when
            alarmLocationCacheService.refreshGoogleLocationCache(alarm);

            // then
            verify(googleClient).getPlaceDetails(new PlaceDetailsCriteria("google-place-id", null, null, null));
            assertThat(alarm.getLatitude()).isEqualTo(37.5663);
            assertThat(alarm.getLongitude()).isEqualTo(126.9779);
            assertThat(alarm.getAddress()).isEqualTo("서울특별시 중구 세종대로 110");
            assertThat(alarm.getLocationCachedAt()).isEqualTo(FIXED_NOW);
        }
    }

    private AlarmEntity googlePlaceAlarm(LocalDateTime cachedAt) {
        AlarmEntity alarm = AlarmFixture.ALARM_01.toMockEntity();
        alarm.updateGooglePlaceLocation(
            "google-place-id",
            alarm.getAddress(),
            alarm.getLatitude(),
            alarm.getLongitude(),
            cachedAt
        );
        return alarm;
    }
}
