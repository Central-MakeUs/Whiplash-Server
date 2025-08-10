package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.etc.OccurrencePushInfo;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmInfoPreviewResponse;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmRemainingOffCountResponse;
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
import akuma.whiplash.global.util.date.DateUtil;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final int WEEKLY_OFF_LIMIT = 2;

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

    @Override
    public List<OccurrencePushInfo> getPreNotificationTargets(LocalDateTime startInclusive, LocalDateTime endInclusive) {
        LocalDate startDate = startInclusive.toLocalDate();
        LocalTime startTime = startInclusive.toLocalTime();
        LocalDate endDate   = endInclusive.toLocalDate();
        LocalTime endTime   = endInclusive.toLocalTime();

        // startDate == endDate -> 검색 범위가 같은 날짜 안이면 단일 쿼리
        if (startDate.equals(endDate)) {
            return alarmOccurrenceRepository.findPreNotificationTargetsSameDay(
                startDate, startTime, endTime, DeactivateType.NONE
            );
        }

        //  startDate < endDate -> 검색 범위가 다음 날로 넘어가면 두 구간 합집합
        List<OccurrencePushInfo> part1 = alarmOccurrenceRepository.findPreNotificationTargetsFromTime(
            startDate, startTime, DeactivateType.NONE
        ); // [startDate startTime ~ 23:59:59]
        List<OccurrencePushInfo> part2 = alarmOccurrenceRepository.findPreNotificationTargetsUntilTime(
            endDate, endTime, DeactivateType.NONE
        );// [endDate 00:00:00 ~ endTime]

        // 중복 제거(혹시 모를 중복 대비)
        return Stream.concat(part1.stream(), part2.stream())
            .collect(Collectors.collectingAndThen(
                Collectors.toMap(OccurrencePushInfo::occurrenceId, x -> x, (a, b) -> a),
                m -> new ArrayList<>(m.values())
            ));
    }

    @Override
    public AlarmRemainingOffCountResponse getWeeklyRemainingOffCount(Long memberId) {
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDateTime weekStart = monday.atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        long offCount = alarmOffLogRepository.countByMemberIdAndCreatedAtBetween(
            memberId, weekStart, now
        );

        int count = (int) Math.max(0, WEEKLY_OFF_LIMIT - offCount);

        return AlarmRemainingOffCountResponse.builder()
            .remainingOffCount(count)
            .build();
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
        LocalDate firstDate = DateUtil.getNextOccurrenceDate(repeatSet, today);
        LocalDate secondDate = DateUtil.getNextOccurrenceDate(repeatSet, firstDate.plusDays(1));
        LocalDate thirdDate = DateUtil.getNextOccurrenceDate(repeatSet, secondDate.plusDays(1));

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
            .firstUpcomingDayOfWeek(DateUtil.getKoreanDayOfWeek(resolvedFirstUpcomingDate))
            .secondUpcomingDay(resolvedSecondUpcomingDate)
            .secondUpcomingDayOfWeek(DateUtil.getKoreanDayOfWeek(resolvedSecondUpcomingDate))
            .build();
    }
}



