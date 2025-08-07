package akuma.whiplash.domains.alarm.domain.service;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.REPEAT_DAYS_NOT_CONFIG;

import akuma.whiplash.domains.alarm.application.dto.response.AlarmInfoPreviewResponse;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOffLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.code.CommonErrorCode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmQueryServiceImpl implements AlarmQueryService {

    private final AlarmRepository alarmRepository;
    private final AlarmOccurrenceRepository alarmOccurrenceRepository;
    private final AlarmOffLogRepository alarmOffLogRepository;
    private final MemberRepository memberRepository;

    @Transactional
    @Override
    public List<AlarmInfoPreviewResponse> getAlarms(Long memberId) {
        // 1. 요청한 회원 존재 여부 검증
        memberRepository.findById(memberId)
            .orElseThrow(() -> ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        // 2. 알람 목록 조회
        List<AlarmEntity> alarms = alarmRepository.findAllByMemberId(memberId);
        LocalDate today = LocalDate.now();

        return alarms.stream()
            .map(alarm -> buildPreviewResponse(alarm, today, memberId))
            .toList();
    }

    private AlarmInfoPreviewResponse buildPreviewResponse(AlarmEntity alarm, LocalDate today, Long memberId) {
        // 1. 가장 최근 OFF 또는 CHECKIN 이력 조회
        Optional<AlarmOccurrenceEntity> recentOccurrenceOpt =
            alarmOccurrenceRepository.findTopByAlarmIdAndDeactivateTypeInOrderByDateDescTimeDesc(
                alarm.getId(),
                List.of(DeactivateType.OFF, DeactivateType.CHECKIN)
            );

        // 2. 반복 요일을 DayOfWeek로 변환
        Set<DayOfWeek> repeatSet = alarm.getRepeatDays().stream()
            .map(Weekday::getDayOfWeek)
            .collect(Collectors.toSet());

        // 3. 오늘 기준 알람 예정일 계산: 첫 번째/두 번째/세 번째 텀
        LocalDate firstDate = calculateNextRepeatDate(repeatSet, today);
        LocalDate secondDate = calculateNextRepeatDate(repeatSet, firstDate.plusDays(1));
        LocalDate thirdDate = calculateNextRepeatDate(repeatSet, secondDate.plusDays(1));

        /**
         * 최근 알람 비활성화(OFF) 이력이 오늘의 알람에 대해 발생한 경우,
         * 현재(firstDate)는 이미 꺼진 상태이므로 다음 알람(firstUpcomingDate)은 그 다음 텀(secondDate)이 되어야 한다.
         *
         * 예시)
         *   - 알람 반복 요일이 월, 수, 금일 때
         *   - 오늘이 수요일이고 최근 OFF 이력이 수요일(firstDate)로 존재한다면,
         *     → 이번 수요일 알람은 꺼졌으므로 다음 울림일은 금요일(secondDate)이 되어야 함.
         *
         * 또한,
         *   - 최근 끄기 이력이 OFF 타입인 경우에만 isToggleOn = false (수동 OFF에만 토글 반영)
         *   - CHECKIN은 출석으로 꺼졌기 때문에 토글은 그대로 유지됨 (isToggleOn = true)
         */

        // 4. 다음 알람일(firstUpcomingDate), 다음+1 알람일(secondUpcomingDate) 결정
        boolean isCurrentDeactivated = recentOccurrenceOpt
            .map(occ -> occ.getDate().equals(firstDate))
            .orElse(false);

        boolean isOff = recentOccurrenceOpt
            .map(occ -> occ.getDeactivateType() == DeactivateType.OFF)
            .orElse(false);

        // 현재 비활성화 상태이고, OFF로 꺼졌으면 toggle 비활성화
        boolean isToggleOn = !(isCurrentDeactivated && isOff);

        // final로 선언된 upcomingDate
        final LocalDate resolvedFirstUpcomingDate = isCurrentDeactivated ? secondDate : firstDate;
        final LocalDate resolvedSecondUpcomingDate = isCurrentDeactivated ? thirdDate : secondDate;

        // 5. 회원의 이번 주 남은 알람 끄기 횟수 계산
        long remainingOffCount = calculateRemainingOffCount(memberId, alarm.getId());

        return AlarmInfoPreviewResponse.builder()
            .alarmId(alarm.getId())
            .alarmPurpose(alarm.getAlarmPurpose())
            .repeatsDays(
                alarm.getRepeatDays().stream()
                    .map(Weekday::getDescription)
                    .toList()
            )
            .time(alarm.getTime().toString())
            .address(alarm.getAddress())
            .latitude(alarm.getLatitude())
            .longitude(alarm.getLongitude())
            .isToggleOn(isToggleOn)
            .firstUpcomingDay(resolvedFirstUpcomingDate)
            .firstUpcomingDayOfWeek(getKoreanDayOfWeek(resolvedFirstUpcomingDate))
            .secondUpcomingDay(resolvedSecondUpcomingDate)
            .secondUpcomingDayOfWeek(getKoreanDayOfWeek(resolvedSecondUpcomingDate))
            .remainingOffCount(remainingOffCount)
            .build();
    }

    /**
     * 주어진 날짜(fromDate)부터 7일 이내 반복 요일 중 가장 빠른 날짜를 반환합니다.
     *
     * @param repeatDays 알람 반복 요일 (예: 월, 수, 금)
     * @param fromDate 기준 날짜
     * @return repeatDays에 해당하는 가장 가까운 알람 발생일
     */
    private LocalDate calculateNextRepeatDate(Set<DayOfWeek> repeatDays, LocalDate fromDate) {
        for (int i = 0; i < 7; i++) {
            LocalDate candidate = fromDate.plusDays(i);
            if (repeatDays.contains(candidate.getDayOfWeek())) {
                return candidate;
            }
        }
        throw ApplicationException.from(REPEAT_DAYS_NOT_CONFIG);
    }

    /**
     * 이번 주 월요일부터 현재까지의 OFF 로그를 기반으로 남은 끄기 횟수 계산
     */
    private long calculateRemainingOffCount(Long memberId, Long alarmId) {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDateTime weekStart = monday.atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        long offCount = alarmOffLogRepository.countByMemberIdAndCreatedAtBetween(
            memberId, weekStart, now
        );

        return Math.max(0, 2 - offCount);
    }

    private String getKoreanDayOfWeek(LocalDate date) {
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
}



