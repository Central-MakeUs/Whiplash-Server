package akuma.whiplash.domains.alarm.domain.service;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.*;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
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
import akuma.whiplash.global.util.date.DateUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
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

    private static final double CHECKIN_RADIUS_METERS = 100.0;

    @Override
    public void createAlarm(AlarmRegisterRequest request, Long memberId) {
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
        LocalDate clientDate = clientNow.toLocalDate(); // 클라이언트 기준 날짜

        // 1. 클라이언트와 서버 시간 간 불일치 검사
        validateClockSkew(clientNow, serverNow);

        // 2. 알람 조회 및 소유자 검증
        AlarmEntity findAlarm = findAlarmById(alarmId);
        validAlarmOwner(findAlarm.getMember().getId(), memberId);

        // 3. 이번 주 시작~끝 날짜 계산 (주간 OFF 제한용)
        LocalDate weekStart = DateUtil.getWeekStartDate(clientDate);
        LocalDate weekEnd = DateUtil.getWeekEndDate(weekStart);

        // 4. 이번 주 끈 횟수 조회
        long weeklyOffCount = alarmOffLogRepository.countByMemberIdAndCreatedAtBetween(
            memberId,
            weekStart.atStartOfDay(),
            weekEnd.plusDays(1).atStartOfDay()
        );

        // 5. 제한 초과 시 예외 발생
        if (weeklyOffCount >= 2) {
            throw ApplicationException.from(ALARM_OFF_LIMIT_EXCEEDED);
        }

        // 6. 오늘 알람 발생 내역 조회
        Optional<AlarmOccurrenceEntity> todayOccurrenceOpt =
            alarmOccurrenceRepository.findByAlarmIdAndDate(alarmId, clientDate);

        // 7. 알람이 울렸고 비활성화되지 않은 상태라면 → 다음 알람을 대상으로 설정
        boolean isAfterRinging = todayOccurrenceOpt
            .filter(o -> o.isAlarmRinging() && o.getDeactivateType() == DeactivateType.NONE)
            .map(o -> clientNow.isAfter(o.getTime().atDate(clientDate)))
            .orElse(false);

        // 8. 꺼야 할 알람 날짜 계산
        LocalDate searchStartDate = isAfterRinging ? clientDate.plusDays(1) : clientDate;
        List<DayOfWeek> repeatDays = findAlarm.getRepeatDays().stream()
            .map(Weekday::getDayOfWeek)
            .sorted()
            .toList();
        LocalDate offTargetDate = DateUtil.getNextOccurrenceDate(repeatDays, searchStartDate);

        // 9. 같은 주인지 검증
        validSameWeek(offTargetDate, clientDate);

        // 10. 발생 내역 조회 또는 생성
        AlarmOccurrenceEntity targetOccurrence = alarmOccurrenceRepository
            .findByAlarmIdAndDate(alarmId, offTargetDate)
            .orElseGet(() -> alarmOccurrenceRepository.save(
                AlarmMapper.mapToAlarmOccurrenceForDate(findAlarm, offTargetDate)));

        // 11. 이미 꺼져 있다면 예외
        if (targetOccurrence.getDeactivateType() != DeactivateType.NONE) {
            throw ApplicationException.from(ALREADY_DEACTIVATED);
        }

        // 12. 발생 내역 상태 변경 및 로그 기록
        targetOccurrence.deactivate(DeactivateType.OFF, serverNow);
        AlarmOffLogEntity alarmOffLog = AlarmMapper.mapToAlarmOffLogEntity(findAlarm, findAlarm.getMember());

        alarmOccurrenceRepository.save(targetOccurrence);
        alarmOffLogRepository.save(alarmOffLog);

        // 13. 다음 알람 울림 날짜 계산 및 응답 구성
        LocalDate reactivateDate = DateUtil.getNextOccurrenceDate(repeatDays, offTargetDate.plusDays(1));
        int remainingCount = (int) (2 - (weeklyOffCount + 1));

        return AlarmOffResultResponse.builder()
            .offTargetDate(offTargetDate)
            .offTargetDayOfWeek(DateUtil.getKoreanDayOfWeek(offTargetDate))
            .reactivateDate(reactivateDate)
            .reactivateDayOfWeek(DateUtil.getKoreanDayOfWeek(reactivateDate))
            .remainingOffCount(remainingCount)
            .build();
    }

    @Override
    public void removeAlarm(Long memberId, Long alarmId, String reason) {
        // 1. 알람 조회 및 소유자 검증
        AlarmEntity alarm = findAlarmById(alarmId);
        validAlarmOwner(memberId, alarm.getMember().getId());

        // 2. 삭제 사유를 Google Sheets에 로그로 기록
        logDeleteReason(alarm.getAlarmPurpose(), reason);

        // 3. 알람 발생 내역 전체 조회 및 관련 로그 제거
        List<AlarmOccurrenceEntity> occurrences = alarmOccurrenceRepository.findAllByAlarmId(alarmId);
        for (AlarmOccurrenceEntity occ : occurrences) {
            alarmRingingLogRepository.deleteAllByAlarmOccurrenceId(occ.getId());
        }

        // 4. 알람 발생 이력, 끈 이력 삭제
        alarmOccurrenceRepository.deleteAll(occurrences);
        alarmOffLogRepository.deleteAllByAlarmId(alarmId);

        // 5. 알람 자체 삭제
        alarmRepository.delete(alarm);
    }


    @Override
    public void checkinAlarm(Long memberId, Long alarmId, AlarmCheckinRequest request) {
        // 1. 알람 조회 및 소유자 검증
        AlarmEntity alarm = findAlarmById(alarmId);
        validAlarmOwner(memberId, alarm.getMember().getId());

        // 2. 오늘 날짜 기준으로 다음 알람 발생 날짜 계산
        LocalDate today = LocalDate.now();
        List<DayOfWeek> repeatDays = alarm.getRepeatDays().stream()
            .map(Weekday::getDayOfWeek)
            .sorted()
            .toList();
        LocalDate targetDate = DateUtil.getNextOccurrenceDate(repeatDays, today);

        // 3. 🔒 요청일과 발생일이 같은 주인지 검증
        validSameWeek(targetDate, today);

        // 4. 해당 날짜의 발생 내역이 없으면 생성
        AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository
            .findByAlarmIdAndDate(alarmId, targetDate)
            .orElseGet(() -> alarmOccurrenceRepository.save(
                AlarmMapper.mapToAlarmOccurrenceForDate(alarm, targetDate)));

        // 5. 이미 끄기/체크인 처리되었으면 예외
        if (occurrence.getDeactivateType() != DeactivateType.NONE) {
            throw ApplicationException.from(ALREADY_DEACTIVATED);
        }

        // 6. 위치 반경 내 도달했는지 검증
        boolean isInRange = isWithinDistance(
            alarm.getLatitude(), alarm.getLongitude(),
            request.latitude(), request.longitude(), CHECKIN_RADIUS_METERS);

        if (!isInRange) {
            throw ApplicationException.from(CHECKIN_OUT_OF_RANGE);
        }

        // 7. 체크인 처리
        occurrence.checkin(LocalDateTime.now());
    }

    /**
     * 위치 인증 반경 내 도착 여부 계산
     *
     * @param lat1 기준 위도 (알람 설정 위치)
     * @param lon1 기준 경도
     * @param lat2 사용자 위도
     * @param lon2 사용자 경도
     * @param radiusMeters 반경(m)
     * @return true if within radius
     */
    private boolean isWithinDistance(double lat1, double lon1, double lat2, double lon2, double radiusMeters) {
        double earthRadius = 6371000; // meters

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = earthRadius * c;

        return distance <= radiusMeters;
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

    private MemberEntity findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> ApplicationException.from(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private AlarmEntity findAlarmById(Long alarmId) {
        return alarmRepository.findById(alarmId)
            .orElseThrow(() -> ApplicationException.from(ALARM_NOT_FOUND));
    }

    private static void validSameWeek(LocalDate offTargetDate, LocalDate clientDate) {
        if (!DateUtil.isSameWeek(offTargetDate, clientDate)) {
            throw ApplicationException.from(NEXT_WEEK_ALARM_DEACTIVATION_NOT_ALLOWED);
        }
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
