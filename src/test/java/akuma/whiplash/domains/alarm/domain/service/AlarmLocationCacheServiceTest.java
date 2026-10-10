package akuma.whiplash.domains.alarm.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.domains.alarm.domain.constant.LocationSource;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.place.domain.client.GoogleClient;
import akuma.whiplash.domains.place.domain.model.PlaceDetailsCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
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
@DisplayName("Google 장소 위치 캐시를 관리한다")
class AlarmLocationCacheServiceTest {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 7, 25, 12, 0);

    @Mock
    private GoogleClient googleClient;
    @Mock
    private TimeProvider timeProvider;
    @Mock
    private AlarmLocationCachePersistenceService alarmLocationCachePersistenceService;
    @Mock
    private AlarmRepository alarmRepository;
    @InjectMocks
    private AlarmLocationCacheService alarmLocationCacheService;

    @Nested
    @DisplayName("Google 장소 위치 캐시 유효성을 판단한다")
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
    @DisplayName("Google 장소 위치 캐시를 갱신한다")
    class ModifyGoogleLocationCacheTest {

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
            alarmLocationCacheService.modifyGoogleLocationCache(alarm);

            // then
            verify(googleClient).getPlaceDetails(new PlaceDetailsCriteria("google-place-id", null, null, null));
            assertThat(alarm.getLatitude()).isEqualTo(37.5663);
            assertThat(alarm.getLongitude()).isEqualTo(126.9779);
            assertThat(alarm.getAddress()).isEqualTo("서울특별시 중구 세종대로 110");
            assertThat(alarm.getLocationCachedAt()).isEqualTo(FIXED_NOW);
            verify(alarmLocationCachePersistenceService)
                .modifyGoogleLocationCache(alarm.getId(), detail, FIXED_NOW);
        }

        @Test
        @DisplayName("실패: 상세 조회 오류는 읽었던 Google 캐시 버전만 정리한다")
        void fail_providerUnavailable() {
            // given
            LocalDateTime previousCachedAt = FIXED_NOW.minusDays(29);
            AlarmEntity alarm = googlePlaceAlarm(previousCachedAt);
            given(timeProvider.now()).willReturn(FIXED_NOW);
            given(googleClient.getPlaceDetails(new PlaceDetailsCriteria("google-place-id", null, null, null)))
                .willThrow(new IllegalStateException("provider unavailable"));

            // when
            // then
            assertThatThrownBy(() -> alarmLocationCacheService.modifyGoogleLocationCache(alarm))
                .isInstanceOf(IllegalStateException.class);
            verify(alarmLocationCachePersistenceService)
                .removeGoogleLocationCache(alarm.getId(), previousCachedAt);
        }
    }

    @Nested
    @DisplayName("getAddressForAlarmList - 알람 목록 주소 조회")
    class GetAddressForAlarmListTest {

        @Test
        @DisplayName("성공: 유효한 핀 주소 캐시는 외부 호출 없이 반환한다")
        void success_cachedUserPinAddress() {
            // given
            AlarmEntity alarm = AlarmFixture.ALARM_01.toMockEntity();
            alarm.updateUserPinAddressCache("서울특별시 중구 세종대로 110", FIXED_NOW.minusDays(28));
            given(timeProvider.now()).willReturn(FIXED_NOW);

            // when
            String address = alarmLocationCacheService.getAddressForAlarmList(alarm);

            // then
            assertThat(address).isEqualTo("서울특별시 중구 세종대로 110");
            verifyNoInteractions(googleClient, alarmLocationCachePersistenceService);
        }

        @Test
        @DisplayName("성공: 만료된 핀 주소는 터치 좌표로 다시 조회하되 목표 좌표는 유지한다")
        void success_refreshExpiredUserPinAddress() {
            // given
            AlarmEntity alarm = AlarmFixture.ALARM_01.toMockEntity();
            alarm.updateUserPinAddressCache("만료된 주소", FIXED_NOW.minusDays(29));
            PlaceDetail detail = new PlaceDetail("서울특별시 중구 세종대로 110", null, null, 37.0, 127.0, "KR");
            given(timeProvider.now()).willReturn(FIXED_NOW);
            given(googleClient.reverseGeocode(alarm.getLatitude(), alarm.getLongitude(), "ko")).willReturn(detail);

            // when
            String address = alarmLocationCacheService.getAddressForAlarmList(alarm);

            // then
            assertThat(address).isEqualTo("서울특별시 중구 세종대로 110");
            assertThat(alarm.getLatitude()).isEqualTo(37.5665);
            assertThat(alarm.getLongitude()).isEqualTo(126.978);
            verify(alarmLocationCachePersistenceService).modifyUserPinAddressCache(
                alarm.getId(), detail.address(), FIXED_NOW
            );
        }

        @Test
        @DisplayName("성공: Google 장소는 Place ID로 갱신한 주소를 추가 DB 조회 없이 반환한다")
        void success_refreshGooglePlaceAddress() {
            // given
            AlarmEntity alarm = googlePlaceAlarm(FIXED_NOW.minusDays(29));
            SelectedPlaceDetail detail = new SelectedPlaceDetail(
                "서울특별시 중구 세종대로 110", 37.5663, 126.9779, "KR", "google-place-id"
            );
            given(timeProvider.now()).willReturn(FIXED_NOW);
            given(googleClient.getPlaceDetails(new PlaceDetailsCriteria("google-place-id", null, null, null)))
                .willReturn(detail);

            // when
            String address = alarmLocationCacheService.getAddressForAlarmList(alarm);

            // then
            assertThat(address).isEqualTo(detail.address());
            verifyNoInteractions(alarmRepository);
            verify(alarmLocationCachePersistenceService)
                .modifyGoogleLocationCache(alarm.getId(), detail, FIXED_NOW);
        }

        @Test
        @DisplayName("성공: 같은 시각에 갱신해도 캐시 버전은 이전보다 한 단계 앞으로 이동한다")
        void success_newCacheVersionOnSameClockTick() {
            // given
            AlarmEntity alarm = googlePlaceAlarm(FIXED_NOW);
            alarm.updateGooglePlaceLocation("google-place-id", null, 37.5665, 126.978, FIXED_NOW);
            SelectedPlaceDetail detail = new SelectedPlaceDetail(
                "서울특별시 중구 세종대로 110", 37.5663, 126.9779, "KR", "google-place-id"
            );
            given(timeProvider.now()).willReturn(FIXED_NOW);
            given(googleClient.getPlaceDetails(new PlaceDetailsCriteria("google-place-id", null, null, null)))
                .willReturn(detail);

            // when
            alarmLocationCacheService.getAddressForAlarmList(alarm);

            // then
            verify(alarmLocationCachePersistenceService)
                .modifyGoogleLocationCache(alarm.getId(), detail, FIXED_NOW.plusNanos(1_000));
        }

        @Test
        @DisplayName("성공: 일반 핀 주소도 같은 시각에 갱신하면 캐시 버전이 증가한다")
        void success_userPinCacheVersionOnSameClockTick() {
            // given
            AlarmEntity alarm = AlarmFixture.ALARM_01.toMockEntity();
            alarm.updateUserPinAddressCache(null, FIXED_NOW);
            PlaceDetail detail = new PlaceDetail("새 주소", null, null, 37.5665, 126.978, "KR");
            given(timeProvider.now()).willReturn(FIXED_NOW);
            given(googleClient.reverseGeocode(alarm.getLatitude(), alarm.getLongitude(), "ko"))
                .willReturn(detail);

            // when
            alarmLocationCacheService.getAddressForAlarmList(alarm);

            // then
            verify(alarmLocationCachePersistenceService)
                .modifyUserPinAddressCache(alarm.getId(), "새 주소", FIXED_NOW.plusNanos(1_000));
        }

        @Test
        @DisplayName("성공: 주소 재조회 실패 시 유효한 Google 목표 좌표는 유지한다")
        void success_googleAddressFailureKeepsValidCoordinates() {
            // given
            AlarmEntity alarm = googlePlaceAlarm(FIXED_NOW.minusDays(1));
            alarm.updateGooglePlaceLocation("google-place-id", null, 37.5665, 126.978, FIXED_NOW.minusDays(1));
            given(timeProvider.now()).willReturn(FIXED_NOW);
            given(googleClient.getPlaceDetails(new PlaceDetailsCriteria("google-place-id", null, null, null)))
                .willThrow(new IllegalStateException("provider unavailable"));

            // when
            String address = alarmLocationCacheService.getAddressForAlarmList(alarm);

            // then
            assertThat(address).isNull();
            assertThat(alarm.getLatitude()).isEqualTo(37.5665);
            assertThat(alarm.getLongitude()).isEqualTo(126.978);
            verifyNoInteractions(alarmLocationCachePersistenceService);
        }

        @Test
        @DisplayName("성공: 역지오코딩과 캐시 정리가 모두 실패해도 목록 주소는 비워 반환한다")
        void success_providerAndCleanupFailure() {
            // given
            AlarmEntity alarm = AlarmFixture.ALARM_01.toMockEntity();
            alarm.updateUserPinAddressCache("만료된 주소", FIXED_NOW.minusDays(29));
            given(timeProvider.now()).willReturn(FIXED_NOW);
            given(googleClient.reverseGeocode(alarm.getLatitude(), alarm.getLongitude(), "ko"))
                .willThrow(new IllegalStateException("provider unavailable"));
            doThrow(new IllegalStateException("database unavailable"))
                .when(alarmLocationCachePersistenceService).removeUserPinAddressCache(
                    alarm.getId(), FIXED_NOW.minusDays(29)
                );

            // when
            String address = alarmLocationCacheService.getAddressForAlarmList(alarm);

            // then
            assertThat(address).isNull();
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
