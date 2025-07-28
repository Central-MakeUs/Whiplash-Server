package akuma.whiplash.domains.alarm.domain.service;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.*;

import akuma.whiplash.domains.alarm.application.dto.request.RegisterAlarmRequest;
import akuma.whiplash.domains.alarm.application.dto.response.AlarmOffResultResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.DeactivateType;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOffLogEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOffLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.exception.ApplicationException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AlarmCommandServiceImpl implements AlarmCommandService {

    private final AlarmRepository alarmRepository;
    private final AlarmOccurrenceRepository alarmOccurrenceRepository;
    private final AlarmOffLogRepository alarmOffLogRepository;
    private final MemberRepository memberRepository;

    @Override
    public void createAlarm(RegisterAlarmRequest request, Long memberId) {
        MemberEntity memberEntity = findMemberById(memberId);

        AlarmEntity alarm = AlarmMapper.mapToAlarmEntity(request, memberEntity);
        alarmRepository.save(alarm);
    }

    @Override
    public CreateAlarmOccurrenceResponse createAlarmOccurrence(Long memberId, Long alarmId) {
        AlarmEntity alarmEntity = findAlarmById(alarmId);

        validAlarmOwner(memberId, alarmEntity.getMember().getId());

        // 각 알람이 울릴 때 알람 발생 내역은 1개만 허용(반복 울림은 alarm_ringing_log로 관리), 오늘 날짜 기준 알람 발생 내역이 이미 존재하면 예외 발생
        boolean alreadyExists = alarmOccurrenceRepository.existsByAlarmIdAndDate(alarmId, LocalDate.now());
        if (alreadyExists) {
            throw ApplicationException.from(ALREADY_OCCURRED_EXISTS);
        }

        AlarmOccurrenceEntity alarmOccurrenceEntity = AlarmMapper.mapToTodayFirstAlarmOccurrenceEntity(alarmEntity);
        alarmOccurrenceRepository.save(alarmOccurrenceEntity);

        return AlarmMapper.mapToCreateAlarmOccurrenceResponse(alarmOccurrenceEntity.getId());
    }

    @Override
    public AlarmOffResultResponse alarmOff(Long memberId, Long alarmId, LocalDateTime clientNow) {
        LocalDateTime serverNow = LocalDateTime.now(); // 서버 기준 현재 시간 (DB 기록용)
        LocalDate clientDate = clientNow.toLocalDate();      // 클라이언트 기준 현재 날짜

        // 요청 날짜와 서버의 날짜가 다르면 다른 날짜의 알람을 끌 수도 있으므로 예외 발생
        validateClockSkew(clientNow, serverNow);

        AlarmEntity alarm = findAlarmById(alarmId);
        MemberEntity member = findMemberById(memberId);
        validAlarmOwner(alarm.getMember().getId(), memberId);

        // 이번 주 시작과 끝 날짜 계산 (주차 단위 끄기 횟수 제한에 사용)
        LocalDate weekStart = clientDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        // 이번 주 끈 횟수 확인 (횟수 초과 시 예외)
        long weeklyOffCount = alarmOffLogRepository.countByAlarmIdAndMemberIdAndCreatedAtBetween(
            alarmId, memberId,
            weekStart.atStartOfDay(),
            weekEnd.plusDays(1).atStartOfDay()
        );

        if (weeklyOffCount >= 2) {
            throw ApplicationException.from(ALARM_OFF_LIMIT_EXCEEDED);
        }

        // 오늘 알람 발생 내역 조회
        Optional<AlarmOccurrenceEntity> todayOccurrenceOpt =
            alarmOccurrenceRepository.findByAlarmIdAndDate(alarmId, clientDate);

        // 다음 텀 알람을 끌지, 아니면 오늘 알람을 끌지 판단하는 변수
        // 오늘 알람이 울렸고, 오늘 울린 알람이 꺼졌으면 true(다음 텀 알람 끄기), 아니라면 false(오늘 알람 끄기)
        boolean isAfterRinging = todayOccurrenceOpt
            .map(o ->
                o.isAlarmRinging()
                && clientNow.isAfter(o.getTime().atDate(clientDate))
                && o.getDeactivateType() != DeactivateType.NONE // 아직 꺼졌는지 확인
            )
            .orElse(false);

        // 🔍 끌 대상 알람 날짜 계산
        LocalDate offTargetDate = isAfterRinging
            ? getNextOccurrenceDate(alarm, clientDate.plusDays(1))  // 울린 후 → 다음 텀 알람을 끈다
            : getNextOccurrenceDate(alarm, clientDate);              // 울리기 전 → 이번 텀 알람을 끈다

        // 🔧 끌 대상 날짜의 알람 발생 내역 조회 (없으면 새로 생성해서 할당)
        AlarmOccurrenceEntity targetOccurrence = alarmOccurrenceRepository
            .findByAlarmIdAndDate(alarmId, offTargetDate)
            .orElseGet(() -> {
                AlarmOccurrenceEntity newOccurrence = AlarmMapper.mapToAlarmOccurrenceForDate(alarm, offTargetDate);
                return alarmOccurrenceRepository.save(newOccurrence); // 새로 만들었으면 저장 필요
            });

        // 이미 꺼진 알람이라면 예외
        if (targetOccurrence.getDeactivateType() != DeactivateType.NONE) {
            throw ApplicationException.from(ALREADY_DEACTIVATED);
        }

        // 상태를 OFF로 변경하고 저장
        targetOccurrence.deactivate(DeactivateType.OFF, serverNow);
        alarmOccurrenceRepository.save(targetOccurrence);

        // 로그 저장 (알람 끈 기록)
        AlarmOffLogEntity alarmOffLog = AlarmMapper.mapToAlarmOffLogEntity(alarm, member);
        alarmOffLogRepository.save(alarmOffLog);

        // 토글 재활성화 날짜 = 끈 알람 다음날
        LocalDate reactivateDate = offTargetDate.plusDays(1);
        String offTargetDayOfWeek = getKoreanDayOfWeek(offTargetDate);
        String reactivateDayOfWeek = getKoreanDayOfWeek(reactivateDate);

        int remainingCount = (int)(2 - weeklyOffCount - 1); // 끄고 난 뒤 남은 횟수

        return AlarmOffResultResponse.builder()
            .offTargetDate(offTargetDate)
            .offTargetDayOfWeek(offTargetDayOfWeek)
            .reactivateDate(reactivateDate)
            .reactivateDayOfWeek(reactivateDayOfWeek)
            .remainingOffCount(remainingCount)
            .build();
    }

    private LocalDate getNextOccurrenceDate(AlarmEntity alarm, LocalDate fromDate) {
        List<DayOfWeek> repeatDays = alarm.getRepeatDays().stream()
            .map(Weekday::getDayOfWeek)
            .sorted()
            .toList();

        for (int i = 0; i < 7; i++) {
            LocalDate candidate = fromDate.plusDays(i);
            if (repeatDays.contains(candidate.getDayOfWeek())) {
                return candidate;
            }
        }

        throw ApplicationException.from(REPEAT_DAYS_NOT_CONFIG);
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

    private MemberEntity findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private AlarmEntity findAlarmById(Long alarmId) {
        return alarmRepository.findById(alarmId)
            .orElseThrow(() -> ApplicationException.from(ALARM_NOT_FOUND));
    }

    private static void validAlarmOwner(Long reqMemberId, Long alarmMemberId) {
        if (!reqMemberId.equals(alarmMemberId)) {
            throw ApplicationException.from(AuthErrorCode.PERMISSION_DENIED);
        }
    }

    private static void validateClockSkew(LocalDateTime clientNow, LocalDateTime serverNow) {
        LocalDate clientDate = clientNow.toLocalDate();
        LocalDate serverDate = serverNow.toLocalDate();

        if (!clientDate.equals(serverDate)) {
            throw ApplicationException.from(INVALID_CLIENT_DATE);
        }
    }
}
