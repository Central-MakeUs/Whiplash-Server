package akuma.whiplash.global.util.date;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.REPEAT_DAYS_NOT_CONFIG;

import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.code.CommonErrorCode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    /**
     * 문자열을 {@link LocalTime}으로 변환합니다.
     * 24시 형태("24:00", "24:30")는 다음 날 0시 형태("00:00", "00:30")로 변환하여 파싱합니다.
     *
     * @param timeString 시각 문자열
     * @return 파싱된 {@link LocalTime}
     * @throws DateTimeParseException 잘못된 형식의 문자열인 경우
     */
    public static LocalTime parseLocalTimeWith24HourSupport(String timeString) {
        String normalized = "";

        try {
            if (timeString == null) {
                log.warn("timeString must not be null");
                throw ApplicationException.from(CommonErrorCode.BAD_REQUEST);
            }

            normalized = timeString;
            if (normalized.startsWith("24:")) {
                normalized = "00" + normalized.substring(2);
            }

        } catch (DateTimeParseException e) {
            log.warn(e.getMessage());
            ApplicationException.from(CommonErrorCode.BAD_REQUEST);
        }

        return LocalTime.parse(normalized);
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

