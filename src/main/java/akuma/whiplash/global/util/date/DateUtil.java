package akuma.whiplash.global.util.date;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.REPEAT_DAYS_NOT_CONFIG;

import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.global.exception.ApplicationException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DateUtil {

    private DateUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 두 날짜가 같은 주(월~일)에 속하는지 비교합니다.
     * ISO-8601 주 기준: 주 시작일은 월요일
     */
    public static boolean isSameWeek(LocalDate date1, LocalDate date2) {
        WeekFields weekFields = WeekFields.of(Locale.KOREA);
        int week1 = date1.get(weekFields.weekOfWeekBasedYear());
        int week2 = date2.get(weekFields.weekOfWeekBasedYear());
        int year1 = date1.get(weekFields.weekBasedYear());
        int year2 = date2.get(weekFields.weekBasedYear());

        return week1 == week2 && year1 == year2;
    }

    /**
     * 해당 날짜가 포함된 주의 시작일(월요일)을 반환합니다.
     */
    public static LocalDate getWeekStartDate(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    /**
     * 해당 날짜가 포함된 주의 종료일(일요일)을 반환합니다.
     */
    public static LocalDate getWeekEndDate(LocalDate date) {
        return getWeekStartDate(date).plusDays(6);
    }

    /**
     * 두 LocalDateTime 값이 같은 날인지 비교합니다.
     */
    public static boolean isSameDay(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        return dateTime1.toLocalDate().isEqual(dateTime2.toLocalDate());
    }

    public static String getKoreanDayOfWeek(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "월요일";
            case TUESDAY -> "화요일";
            case WEDNESDAY -> "수요일";
            case THURSDAY -> "목요일";
            case FRIDAY -> "금요일";
            case SATURDAY -> "토요일";
            case SUNDAY -> "일요일";
        };
    }

    /**
     * 주어진 날짜(fromDate)부터 7일 이내 반복 요일 중 가장 빠른 날짜를 반환합니다.
     *
     * @param repeatDays 알람 반복 요일 (예: 월, 수, 금)
     * @param fromDate 기준 날짜
     * @return repeatDays에 해당하는 가장 가까운 알람 발생일
     */
    public static LocalDate getNextOccurrenceDate(Set<DayOfWeek> repeatDays, LocalDate fromDate) {

        for (int i = 0; i < 7; i++) {
            LocalDate candidate = fromDate.plusDays(i);
            if (repeatDays.contains(candidate.getDayOfWeek())) {
                return candidate;
            }
        }

        throw ApplicationException.from(REPEAT_DAYS_NOT_CONFIG);
    }
}

