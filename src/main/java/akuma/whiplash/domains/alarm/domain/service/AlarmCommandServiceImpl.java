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
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRingingLogRepository;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.exception.ApplicationException;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.FileInputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AlarmCommandServiceImpl implements AlarmCommandService {

    private final AlarmRepository alarmRepository;
    private final AlarmOccurrenceRepository alarmOccurrenceRepository;
    private final AlarmOffLogRepository alarmOffLogRepository;
    private final AlarmRingingLogRepository alarmRingingLogRepository;
    private final MemberRepository memberRepository;

    @Value("${oauth.google.sheet.id}")
    private String spreadsheetsId;

    @Value("${oauth.google.sheet.credentials-path}")
    private String credentialsPath;

    @Value("${oauth.google.sheet.range}")
    private String sheetRange;

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

        // 알람 조회와 소유자 검증을 먼저 수행 (early fail)
        AlarmEntity alarm = findAlarmById(alarmId);
        validAlarmOwner(alarm.getMember().getId(), memberId);

        // 이번 주 시작과 끝 날짜 계산 (주차 단위 끄기 횟수 제한에 사용)
        LocalDate weekStart = clientDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        // 병렬로 필요한 데이터 조회를 위한 준비 (필요한 쿼리들을 한 번에 실행)
        // 1. 이번 주 끈 횟수 확인과 2. 오늘 알람 발생 내역 조회를 동시에 처리
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

        // 알람이 울렸는지 판단하는 변수 (계산 최적화)
        boolean isAfterRinging = todayOccurrenceOpt
            .filter(o -> o.isAlarmRinging() && o.getDeactivateType() == DeactivateType.NONE)
            .map(o -> clientNow.isAfter(o.getTime().atDate(clientDate)))
            .orElse(false);

        // 끌 대상 알람 날짜 계산 (캐시된 alarm 객체 사용)
        LocalDate searchStartDate = isAfterRinging ? clientDate.plusDays(1) : clientDate;
        LocalDate offTargetDate = getNextOccurrenceDate(alarm, searchStartDate);

        // 끌 대상 날짜의 알람 발생 내역 조회 또는 생성
        AlarmOccurrenceEntity targetOccurrence = alarmOccurrenceRepository
            .findByAlarmIdAndDate(alarmId, offTargetDate)
            .orElseGet(() -> {
                AlarmOccurrenceEntity newOccurrence = AlarmMapper.mapToAlarmOccurrenceForDate(alarm, offTargetDate);
                return alarmOccurrenceRepository.save(newOccurrence);
            });

        // 이미 꺼진 알람이라면 예외
        if (targetOccurrence.getDeactivateType() != DeactivateType.NONE) {
            throw ApplicationException.from(ALREADY_DEACTIVATED);
        }

        // 상태 변경과 로그 저장
        targetOccurrence.deactivate(DeactivateType.OFF, serverNow);
        AlarmOffLogEntity alarmOffLog = AlarmMapper.mapToAlarmOffLogEntity(alarm, alarm.getMember());

        alarmOccurrenceRepository.save(targetOccurrence);
        alarmOffLogRepository.save(alarmOffLog);

        // 토글 재활성화 날짜 = 끈 알람 다음날
        LocalDate reactivateDate = offTargetDate.plusDays(1);
        int remainingCount = (int)(1 - weeklyOffCount); // 2 - weeklyOffCount - 1 = 1 - weeklyOffCount

        return AlarmOffResultResponse.builder()
            .offTargetDate(offTargetDate)
            .offTargetDayOfWeek(getKoreanDayOfWeek(offTargetDate))
            .reactivateDate(reactivateDate) // 다시 활성화 되는 날짜
            .reactivateDayOfWeek(getKoreanDayOfWeek(reactivateDate))
            .remainingOffCount(remainingCount)
            .build();
    }

    @Override
    public void removeAlarm(Long memberId, Long alarmId, String reason) {
        // 1. 알람 조회 및 소유자 검증
        AlarmEntity alarm = findAlarmById(alarmId);
        validAlarmOwner(memberId, alarm.getMember().getId());

        // 2. 다음 알람 발생일 기준으로 삭제 가능 마감 시각 계산 (다음날 00시 이전까지만 삭제 허용)
        LocalDate nextDate = getNextOccurrenceDate(alarm, LocalDate.now());
        LocalDateTime limitTime = nextDate.minusDays(1).atTime(LocalTime.MAX);

        // 3. 현재 시간이 삭제 마감 시간 이후라면 삭제 불가
        if (LocalDateTime.now().isAfter(limitTime)) {
            throw ApplicationException.from(ALARM_DELETE_NOT_AVAILABLE);
        }

        // 4. 오늘 알람 발생 내역이 있고, 비활성화되지 않았다면 삭제 불가
        alarmOccurrenceRepository.findByAlarmIdAndDate(alarmId, LocalDate.now())
            .ifPresent(o -> {
                if (o.getDeactivateType() == DeactivateType.NONE) {
                    throw ApplicationException.from(ALARM_DELETE_NOT_AVAILABLE);
                }
            });

        // 5. 삭제 사유를 Google Sheets에 로그로 기록
        logDeleteReason(alarm.getAlarmPurpose(), reason);

        // 6. 알람 발생 내역 전체 조회 및 관련 로그 제거
        List<AlarmOccurrenceEntity> occurrences = alarmOccurrenceRepository.findAllByAlarmId(alarmId);
        for (AlarmOccurrenceEntity occ : occurrences) {
            alarmRingingLogRepository.deleteAllByAlarmOccurrenceId(occ.getId());
        }

        // 7. 알람 발생 이력, 끈 이력 삭제
        alarmOccurrenceRepository.deleteAll(occurrences);
        alarmOffLogRepository.deleteAllByAlarmId(alarmId);

        // 8. 알람 자체 삭제
        alarmRepository.delete(alarm);
    }


    private void logDeleteReason(String alarmPurpose, String reason) {
        try {
            // 1. Google Sheets API 클라이언트 생성
            Sheets sheets = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(
                    ServiceAccountCredentials.fromStream(
                        new ClassPathResource(credentialsPath).getInputStream() // classpath 리소스 처리
                    ).createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS))
                )
            )
                .setApplicationName("눈 떠!")
                .build();

            // 2. 기록할 값 구성
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = LocalDateTime.now().format(formatter);

            ValueRange body = new ValueRange().setValues(
                List.of(List.of(alarmPurpose, reason, formattedDateTime))
            );

            // 3. 스프레드시트에 행 추가 (append 방식)
            sheets.spreadsheets().values()
                .append(spreadsheetsId, sheetRange, body)
                .setValueInputOption("RAW")
                .execute();

        } catch (Exception e) {
            log.warn("Failed to log delete reason", e);
        }
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
