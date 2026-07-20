package akuma.whiplash.global.util.date;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DateUtil Unit Test")
class DateUtilTest {

    @Test
    @DisplayName("오늘이 반복 요일이고 알람 시간이 현재보다 뒤면 오늘 날짜를 반환한다")
    void getNextOccurrenceDate_returnsTodayWhenAlarmTimeIsAfterNow() {
        // given: 2026-03-02(월) 10:00 기준, 알람 10:30, 반복 요일 월/수
        LocalDateTime now = LocalDateTime.of(2026, 3, 2, 10, 0);
        Set<DayOfWeek> repeatDays = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);

        // when
        LocalDate result = DateUtil.getNextOccurrenceDate(repeatDays, now, LocalTime.of(10, 30));

        // then
        assertThat(result).isEqualTo(LocalDate.of(2026, 3, 2));
    }

    @Test
    @DisplayName("오늘이 반복 요일이어도 알람 시간이 현재보다 같거나 빠르면 다음 반복 요일을 반환한다")
    void getNextOccurrenceDate_returnsNextRepeatDayWhenAlarmTimeIsNotAfterNow() {
        // given: 2026-03-02(월) 10:00 기준, 반복 요일 월/수
        LocalDateTime now = LocalDateTime.of(2026, 3, 2, 10, 0);
        Set<DayOfWeek> repeatDays = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);

        // when
        LocalDate resultAtSameTime = DateUtil.getNextOccurrenceDate(repeatDays, now, LocalTime.of(10, 0));
        LocalDate resultBeforeNow = DateUtil.getNextOccurrenceDate(repeatDays, now, LocalTime.of(9, 30));

        // then
        assertThat(resultAtSameTime).isEqualTo(LocalDate.of(2026, 3, 4));
        assertThat(resultBeforeNow).isEqualTo(LocalDate.of(2026, 3, 4));
    }
}
