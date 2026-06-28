package akuma.whiplash.domains.alarm.domain.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AlarmScheduleCalculator Unit Test")
class AlarmScheduleCalculatorTest {

    @Nested
    @DisplayName("getNextOccurrenceDate - 다음 알람 발생일 계산")
    class GetNextOccurrenceDateTest {

        @Test
        @DisplayName("성공: 사용자 timeZone 기준으로 오늘 알람 시간이 남아있으면 오늘을 반환한다")
        void success() {
            // given
            ZonedDateTime now = ZonedDateTime.of(
                LocalDateTime.of(2026, 5, 1, 6, 30),
                ZoneId.of("America/New_York")
            );

            // when
            LocalDate result = AlarmScheduleCalculator.getNextOccurrenceDate(
                Set.of(DayOfWeek.FRIDAY),
                now,
                LocalTime.of(7, 0)
            );

            // then
            assertThat(result).isEqualTo(LocalDate.of(2026, 5, 1));
        }
    }

    @Nested
    @DisplayName("toDefaultZoneLocalDateTime - 기본 Zone 예정 시각 변환")
    class ToDefaultZoneLocalDateTimeTest {

        @Test
        @DisplayName("성공: 사용자 로컬 알람 시간을 서버 기본 Zone 시각으로 변환한다")
        void success() {
            // given
            ZoneId memberZone = ZoneId.of("America/New_York");

            // when
            LocalDateTime result = AlarmScheduleCalculator.toDefaultZoneLocalDateTime(
                LocalDate.of(2026, 5, 1),
                LocalTime.of(7, 0),
                memberZone
            );

            // then
            assertThat(result).isEqualTo(LocalDateTime.of(2026, 5, 1, 20, 0));
        }
    }

    @Nested
    @DisplayName("toEpochMillis - Epoch millis 변환")
    class ToEpochMillisTest {

        @Test
        @DisplayName("성공: 사용자 timeZone 기준 예정 시각을 epoch millis로 변환한다")
        void success() {
            // given
            ZoneId memberZone = ZoneId.of("America/New_York");
            Instant expected = ZonedDateTime.of(
                LocalDateTime.of(2026, 5, 1, 7, 0),
                memberZone
            ).toInstant();

            // when
            long result = AlarmScheduleCalculator.toEpochMillis(
                LocalDate.of(2026, 5, 1),
                LocalTime.of(7, 0),
                memberZone
            );

            // then
            assertThat(result).isEqualTo(expected.toEpochMilli());
        }
    }

    @Nested
    @DisplayName("toScheduledZonedDateTime - DST 전환 시각 변환")
    class ToScheduledZonedDateTimeTest {

        @Test
        @DisplayName("성공: DST 시작으로 존재하지 않는 현지 시각이면 다음 유효 시각으로 예약한다")
        void success_gap() {
            // given
            ZoneId memberZone = ZoneId.of("America/New_York");

            // when
            ZonedDateTime result = AlarmScheduleCalculator.toScheduledZonedDateTime(
                LocalDate.of(2026, 3, 8),
                LocalTime.of(2, 30),
                memberZone
            );

            // then
            assertThat(result.toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 8));
            assertThat(result.toLocalTime()).isEqualTo(LocalTime.of(3, 30));
            assertThat(result.getOffset().getId()).isEqualTo("-04:00");
        }

        @Test
        @DisplayName("성공: DST 종료로 중복되는 현지 시각이면 더 이른 offset의 첫 발생으로 예약한다")
        void success_overlap() {
            // given
            ZoneId memberZone = ZoneId.of("America/New_York");

            // when
            ZonedDateTime result = AlarmScheduleCalculator.toScheduledZonedDateTime(
                LocalDate.of(2026, 11, 1),
                LocalTime.of(1, 30),
                memberZone
            );

            // then
            assertThat(result.toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 1));
            assertThat(result.toLocalTime()).isEqualTo(LocalTime.of(1, 30));
            assertThat(result.getOffset().getId()).isEqualTo("-04:00");
        }
    }
}
