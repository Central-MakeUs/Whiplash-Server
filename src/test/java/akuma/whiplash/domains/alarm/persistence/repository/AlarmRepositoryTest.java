package akuma.whiplash.domains.alarm.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import akuma.whiplash.common.config.PersistenceTest;
import akuma.whiplash.common.fixture.AlarmFixture;
import akuma.whiplash.common.fixture.MemberFixture;
import akuma.whiplash.domains.alarm.domain.constant.AlarmStatus;
import akuma.whiplash.domains.alarm.domain.constant.LocationSource;
import akuma.whiplash.domains.alarm.domain.constant.SoundType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository.AlarmOccurrenceBatchTarget;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@PersistenceTest
class AlarmRepositoryTest {

    @Autowired
    private AlarmRepository alarmRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("findAllByMemberId - 회원 ID로 알람 조회")
    class FindAllByMemberIdTest {

        @Test
        @DisplayName("성공: 회원 ID로 알람을 조회하면 해당 알람이 반환된다")
        void success() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_7.toEntity());
            alarmRepository.save(AlarmFixture.ALARM_07.toEntity(member));

            // when
            List<AlarmEntity> alarms = alarmRepository.findAllByMemberId(member.getId());

            // then
            assertThat(alarms).hasSize(1);
        }

        @Test
        @DisplayName("실패: 등록되지 않은 회원 ID로 알람을 조회하면 비어있는 목록이 반환된다")
        void fail_memberNotFound() {
            // when
            List<AlarmEntity> alarms = alarmRepository.findAllByMemberId(999L);

            // then
            assertThat(alarms).isEmpty();
        }
    }

    @Nested
    @DisplayName("findBatchTargetsByRepeatDaysLikeAndStatus - 배치 대상 알람 조회")
    class FindBatchTargetsByRepeatDaysLikeAndStatusTest {

        @Test
        @DisplayName("성공: 반복 요일이 일치하는 활성 알람의 ID와 시간만 조회한다")
        void success() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_7.toEntity());
            AlarmEntity activeMondayAlarm = alarmRepository.save(AlarmFixture.ALARM_07.toEntity(member));
            alarmRepository.save(AlarmEntity.builder()
                .member(member)
                .alarmPurpose("주말 알람")
                .time(LocalTime.of(9, 0))
                .repeatDays(List.of(Weekday.SATURDAY))
                .soundType(SoundType.VIBRATION_ONLY)
                .latitude(37.6019)
                .longitude(127.0413)
                .address("서울특별시 성북구 정릉로 77")
                .status(AlarmStatus.ACTIVE)
                .build());
            alarmRepository.save(AlarmEntity.builder()
                .member(member)
                .alarmPurpose("삭제된 월요일 알람")
                .time(LocalTime.of(10, 0))
                .repeatDays(List.of(Weekday.MONDAY))
                .soundType(SoundType.VIBRATION_ONLY)
                .latitude(37.6019)
                .longitude(127.0413)
                .address("서울특별시 성북구 정릉로 77")
                .status(AlarmStatus.DELETED)
                .build());

            // when
            List<AlarmOccurrenceBatchTarget> targets = alarmRepository.findBatchTargetsByRepeatDaysLikeAndStatus(
                "\"MONDAY\"",
                AlarmStatus.ACTIVE.name()
            );

            // then
            assertThat(targets).hasSize(1);
            assertThat(targets.get(0).getAlarmId()).isEqualTo(activeMondayAlarm.getId());
            assertThat(targets.get(0).getAlarmTime()).isEqualTo(activeMondayAlarm.getTime());
        }
    }

    @Nested
    @DisplayName("updateLocationCachesBySourceAndCachedAtBefore - 만료 위치 캐시 정리")
    class ClearLocationCachesBySourceAndCachedAtBeforeTest {

        @Test
        @DisplayName("성공: 만료된 Google 장소 캐시만 삭제하고 Place ID는 남긴다")
        void success() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_8.toEntity());
            AlarmEntity googlePlaceAlarm = AlarmFixture.ALARM_08.toEntity(member);
            googlePlaceAlarm.updateGooglePlaceLocation(
                "google-place-id",
                googlePlaceAlarm.getAddress(),
                googlePlaceAlarm.getLatitude(),
                googlePlaceAlarm.getLongitude(),
                LocalDateTime.of(2026, 6, 26, 12, 0)
            );
            AlarmEntity legacyAlarm = AlarmFixture.ALARM_09.toEntity(member);
            alarmRepository.saveAndFlush(googlePlaceAlarm);
            alarmRepository.saveAndFlush(legacyAlarm);

            // when
            int clearedCount = alarmRepository.updateLocationCachesBySourceAndCachedAtBefore(
                LocationSource.GOOGLE_PLACE,
                LocalDateTime.of(2026, 7, 25, 12, 0)
            );
            entityManager.clear();

            // then
            AlarmEntity clearedAlarm = alarmRepository.findById(googlePlaceAlarm.getId()).orElseThrow();
            AlarmEntity retainedLegacyAlarm = alarmRepository.findById(legacyAlarm.getId()).orElseThrow();
            assertThat(clearedCount).isEqualTo(1);
            assertThat(clearedAlarm.getGooglePlaceId()).isEqualTo("google-place-id");
            assertThat(clearedAlarm.getLatitude()).isNull();
            assertThat(clearedAlarm.getLongitude()).isNull();
            assertThat(clearedAlarm.getAddress()).isNull();
            assertThat(clearedAlarm.getLocationCachedAt()).isNull();
            assertThat(retainedLegacyAlarm.getLatitude()).isNotNull();
        }
    }

    @Nested
    @DisplayName("updateExpiredUserPinAddressCaches - 일반 핀 주소 캐시 정리")
    class ClearExpiredUserPinAddressCachesTest {

        @Test
        @DisplayName("성공: 만료된 핀 주소만 삭제하고 목표 좌표와 Google 장소 캐시는 유지한다")
        void success() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_8.toEntity());
            AlarmEntity userPin = AlarmFixture.ALARM_08.toEntity(member);
            userPin.updateUserPinAddressCache("만료된 주소", LocalDateTime.of(2026, 6, 26, 12, 0));
            AlarmEntity googlePlace = AlarmFixture.ALARM_09.toEntity(member);
            googlePlace.updateGooglePlaceLocation(
                "google-place-id", "Google 주소", googlePlace.getLatitude(), googlePlace.getLongitude(),
                LocalDateTime.of(2026, 6, 26, 12, 0)
            );
            alarmRepository.saveAndFlush(userPin);
            alarmRepository.saveAndFlush(googlePlace);

            // when
            int cleared = alarmRepository.updateExpiredUserPinAddressCaches(
                LocationSource.USER_PIN, LocalDateTime.of(2026, 7, 25, 12, 0)
            );
            entityManager.clear();

            // then
            AlarmEntity refreshedPin = alarmRepository.findById(userPin.getId()).orElseThrow();
            AlarmEntity retainedGooglePlace = alarmRepository.findById(googlePlace.getId()).orElseThrow();
            assertThat(cleared).isEqualTo(1);
            assertThat(refreshedPin.getAddress()).isNull();
            assertThat(refreshedPin.getLocationCachedAt()).isNull();
            assertThat(refreshedPin.getLatitude()).isEqualTo(userPin.getLatitude());
            assertThat(refreshedPin.getLongitude()).isEqualTo(userPin.getLongitude());
            assertThat(retainedGooglePlace.getAddress()).isEqualTo("Google 주소");
        }
    }

    @Nested
    @DisplayName("조건부 위치 캐시 정리 - 동시 갱신 보호")
    class ConditionalLocationCacheCleanupTest {

        @Test
        @DisplayName("성공: 다른 요청이 갱신한 일반 핀 주소는 오래된 요청이 지우지 않는다")
        void success_userPinRefreshWins() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_8.toEntity());
            AlarmEntity pin = AlarmFixture.ALARM_08.toEntity(member);
            LocalDateTime oldCachedAt = LocalDateTime.of(2026, 6, 26, 12, 0);
            LocalDateTime newCachedAt = LocalDateTime.of(2026, 7, 25, 12, 0);
            pin.updateUserPinAddressCache("만료된 주소", oldCachedAt);
            alarmRepository.saveAndFlush(pin);
            pin.updateUserPinAddressCache("새 주소", newCachedAt);
            entityManager.flush();

            // when
            int cleared = alarmRepository.updateUserPinAddressCacheToEmptyIfUnchanged(
                pin.getId(), LocationSource.USER_PIN, oldCachedAt
            );
            entityManager.clear();

            // then
            AlarmEntity retained = alarmRepository.findById(pin.getId()).orElseThrow();
            assertThat(cleared).isZero();
            assertThat(retained.getAddress()).isEqualTo("새 주소");
            assertThat(retained.getLatitude()).isEqualTo(pin.getLatitude());
            assertThat(retained.getLongitude()).isEqualTo(pin.getLongitude());
        }

        @Test
        @DisplayName("성공: 다른 요청이 갱신한 Google 장소 좌표와 주소는 오래된 요청이 지우지 않는다")
        void success_googlePlaceRefreshWins() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_8.toEntity());
            AlarmEntity place = AlarmFixture.ALARM_08.toEntity(member);
            LocalDateTime oldCachedAt = LocalDateTime.of(2026, 6, 26, 12, 0);
            LocalDateTime newCachedAt = LocalDateTime.of(2026, 7, 25, 12, 0);
            place.updateGooglePlaceLocation("google-place-id", "만료된 주소", 37.5, 127.1, oldCachedAt);
            alarmRepository.saveAndFlush(place);
            place.updateGooglePlaceLocation("google-place-id", "새 주소", 37.6, 127.2, newCachedAt);
            entityManager.flush();

            // when
            int cleared = alarmRepository.updateGoogleLocationCacheToEmptyIfUnchanged(
                place.getId(), LocationSource.GOOGLE_PLACE, oldCachedAt
            );
            entityManager.clear();

            // then
            AlarmEntity retained = alarmRepository.findById(place.getId()).orElseThrow();
            assertThat(cleared).isZero();
            assertThat(retained.getAddress()).isEqualTo("새 주소");
            assertThat(retained.getLatitude()).isEqualTo(37.6);
            assertThat(retained.getLongitude()).isEqualTo(127.2);
        }

        @Test
        @DisplayName("성공: 이전 캐시 시각이 없는 일반 핀도 조건부로 정리할 수 있다")
        void success_userPinWithoutPreviousCache() {
            // given
            MemberEntity member = memberRepository.save(MemberFixture.MEMBER_8.toEntity());
            AlarmEntity pin = AlarmFixture.ALARM_08.toEntity(member);
            pin.clearUserPinAddressCache();
            alarmRepository.saveAndFlush(pin);

            // when
            int cleared = alarmRepository.updateUserPinAddressCacheToEmptyIfUnchanged(
                pin.getId(), LocationSource.USER_PIN, null
            );

            // then
            assertThat(cleared).isEqualTo(1);
        }
    }
}
