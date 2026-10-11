package akuma.whiplash.domains.alarm.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.request.PlaceRequest;
import akuma.whiplash.domains.alarm.domain.constant.LocationSource;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AlarmMapper - 알람 위치 출처 매핑")
class AlarmMapperTest {

    private static final LocalDateTime CACHED_AT = LocalDateTime.of(2026, 10, 7, 12, 0);

    @Nested
    @DisplayName("mapToAlarmEntity - 알람 등록 위치 매핑")
    class MapToAlarmEntityTest {

        @Test
        @DisplayName("성공: 일반 지도 핀은 터치 좌표를 유지하고 주소 캐시는 선택적으로 저장한다")
        void success_userPin() {
            // given
            var request = request(new PlaceRequest(null, 37.5665, 126.978, null, null));

            // when
            AlarmEntity alarm = AlarmMapper.mapToAlarmEntity(request, MemberFixture.MEMBER_9.toMockEntity(), CACHED_AT);

            // then
            assertThat(alarm.getLocationSource()).isEqualTo(LocationSource.USER_PIN);
            assertThat(alarm.getLatitude()).isEqualTo(37.5665);
            assertThat(alarm.getLongitude()).isEqualTo(126.978);
            assertThat(alarm.getAddress()).isNull();
            assertThat(alarm.getGooglePlaceId()).isNull();
            assertThat(alarm.getLocationCachedAt()).isNull();
        }

        @Test
        @DisplayName("성공: Google 장소는 기존 Place ID와 주소 캐시를 유지한다")
        void success_googlePlace() {
            // given
            var request = request(new PlaceRequest("서울특별시 중구 세종대로 110", 37.5665, 126.978, "ChIJ", null));

            // when
            AlarmEntity alarm = AlarmMapper.mapToAlarmEntity(request, MemberFixture.MEMBER_9.toMockEntity(), CACHED_AT);

            // then
            assertThat(alarm.getLocationSource()).isEqualTo(LocationSource.GOOGLE_PLACE);
            assertThat(alarm.getGooglePlaceId()).isEqualTo("ChIJ");
            assertThat(alarm.getAddress()).isEqualTo("서울특별시 중구 세종대로 110");
            assertThat(alarm.getLocationCachedAt()).isEqualTo(CACHED_AT);
        }
    }

    private AlarmRegisterRequest request(PlaceRequest place) {
        return new AlarmRegisterRequest(place, "출근", LocalTime.of(8, 0), List.of("MONDAY"), "KARINA_SCOLDING");
    }
}
