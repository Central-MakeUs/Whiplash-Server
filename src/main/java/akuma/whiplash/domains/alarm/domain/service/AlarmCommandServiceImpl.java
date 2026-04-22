package akuma.whiplash.domains.alarm.domain.service;

import static akuma.whiplash.domains.alarm.exception.AlarmErrorCode.*;

import akuma.whiplash.domains.alarm.application.dto.request.AlarmCheckinRequest;
import akuma.whiplash.domains.alarm.application.dto.request.AlarmRegisterRequest;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmOccurrenceResponse;
import akuma.whiplash.domains.alarm.application.dto.response.CreateAlarmResponse;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.OccurrenceStatus;
import akuma.whiplash.domains.alarm.domain.constant.Weekday;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmRingingLogEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOffLogRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRingingLogRepository;
import akuma.whiplash.domains.auth.exception.AuthErrorCode;
import akuma.whiplash.domains.member.exception.MemberErrorCode;
import akuma.whiplash.domains.member.persistence.entity.MemberEntity;
import akuma.whiplash.domains.member.persistence.repository.MemberRepository;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.service.ArchiveService;
import akuma.whiplash.global.util.date.DateUtil;
import akuma.whiplash.infrastructure.redis.RingingAlarmRedisRepository;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final ArchiveService archiveService;
    private final RingingAlarmRedisRepository ringingAlarmRedisRepository;

    @Value("${oauth.google.sheet.id}")
    private String spreadsheetsId;

    @Value("${oauth.google.sheet.credentials-path}")
    private String credentialsPath;

    @Value("${oauth.google.sheet.range}")
    private String sheetRange;

    private static final double CHECKIN_RADIUS_METERS = 100.0;

    @Override
    public CreateAlarmResponse createAlarm(AlarmRegisterRequest request, Long memberId) {
        MemberEntity memberEntity = findMemberById(memberId);

        boolean exists = alarmRepository.existsByMemberIdAndAlarmPurpose(memberId, request.alarmPurpose());
        if (exists) {
            throw ApplicationException.from(DUPLICATE_ALARM_PURPOSE);
        }

        // 1. 알람 엔티티 생성 및 저장
        AlarmEntity alarm = AlarmMapper.mapToAlarmEntity(request, memberEntity);
        alarmRepository.save(alarm);

        // 2. 다음 알람 발생 날짜 계산
        Set<DayOfWeek> repeatDays = alarm.getRepeatDays().stream()
            .map(Weekday::getDayOfWeek)
            .collect(Collectors.toSet());
        
        // 현재 시각 기준으로 가장 가까운 발생일 계산 (오늘 포함 여부는 getNextOccurrenceDate 내부 로직 따름)
        LocalDate nextDate = DateUtil.getNextOccurrenceDate(repeatDays, LocalDateTime.now(), alarm.getTime());
        LocalDateTime nextScheduledTime = LocalDateTime.of(nextDate, alarm.getTime());

        // 3. 첫 알람 발생 내역 생성 및 저장
        AlarmOccurrenceEntity occurrence = AlarmMapper.mapToFirstAlarmOccurrenceEntity(alarm, nextDate, alarm.getTime());
        alarmOccurrenceRepository.save(occurrence);

        // 4. 알람의 다음 예정 시각 업데이트
        alarm.updateNextScheduledTime(nextScheduledTime);

        return AlarmMapper.mapToCreateAlarmResponse(alarm, occurrence);
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
    public void removeAlarm(Long memberId, Long alarmId, String reason) {
        // 1-1. 알람 조회 및 소유자 검증
        AlarmEntity alarm = findAlarmById(alarmId);
        validAlarmOwner(memberId, alarm.getMember().getId());

        // 1-2. 울리고 있을 수 있으므로 Redis Sorted Set에서 먼저 제거
        ringingAlarmRedisRepository.remove(alarmId, memberId);

        // 1-3. 삭제할 데이터 삭제 전 아카이빙
        archiveService.archiveAlarmWithRelations(alarmId);

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

    // TODO: 사용자가 알람이 울리는 날 알람을 끄지 않아 그 다음날로 넘어간 경우에 어떻게 처리할건지
    @Override
    public void checkinAlarm(Long memberId, Long alarmId, AlarmCheckinRequest request) {
        // 1. 알람 조회 및 소유자 검증
        AlarmEntity alarm = findAlarmById(alarmId);
        validAlarmOwner(memberId, alarm.getMember().getId());

        // 2. 오늘 날짜 기준으로 다음 알람 발생 날짜 계산
        LocalDate today = LocalDate.now();
        Set<DayOfWeek> repeatDays = alarm.getRepeatDays().stream()
            .map(Weekday::getDayOfWeek)
            .collect(Collectors.toSet());

        // TODO: 요청일보다 이전 날짜에 울려야할 알람을 꺼야하는 경우도 처리해야함(사용자가 알람을 안꺼서)
        LocalDate targetDate = DateUtil.getNextOccurrenceDate(repeatDays, today);

        // 3. 요청일과 끄려고 하는 날짜가 같은 주인지 검증
        validSameWeek(targetDate, today);

        // 4. 해당 날짜의 발생 내역이 없으면 생성
        AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository
            .findByAlarmIdAndDate(alarmId, targetDate)
            .orElseGet(() -> alarmOccurrenceRepository.save(
                AlarmMapper.mapToAlarmOccurrenceForDate(alarm, targetDate)));

        // 5. 이미 처리된 회차이면 예외
        if (occurrence.getStatus() != OccurrenceStatus.SCHEDULED
                && occurrence.getStatus() != OccurrenceStatus.RINGING) {
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

        // 체크인으로 알람이 비활성화됐으므로 Redis Sorted Set에서 제거.
        // 항목이 없어도 ZREM은 no-op이므로 항상 안전하게 호출한다.
        ringingAlarmRedisRepository.remove(alarmId, memberId);
    }

    @Override
    public void ringAlarm(Long memberId, Long alarmId) {
        AlarmEntity alarm = findAlarmById(alarmId);
        validAlarmOwner(memberId, alarm.getMember().getId());

        // 아직 처리되지 않은 알람 발생 이력 중 가장 최근 것을 가져옴.
        AlarmOccurrenceEntity occurrence = alarmOccurrenceRepository
            .findTopByAlarmIdAndStatusInOrderByOccurrenceDateDescOccurrenceTimeDesc(
                alarmId,
                List.of(OccurrenceStatus.SCHEDULED, OccurrenceStatus.RINGING)
            )
            .orElseThrow(() -> ApplicationException.from(ALARM_OCCURRENCE_NOT_FOUND));

        // 아직 알람이 울릴 시간이 아니라면 예외 발생
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledDateTime = LocalDateTime.of(occurrence.getOccurrenceDate(), occurrence.getOccurrenceTime());
        if (now.isBefore(scheduledDateTime)) {
            throw ApplicationException.from(NOT_ALARM_TIME);
        }

        // 알람 울림 로그 생성
        int ringIndex = occurrence.ring();
        AlarmRingingLogEntity log = AlarmMapper.mapToAlarmRingingLogEntity(
            occurrence,
            ringIndex,
            now
        );
        alarmRingingLogRepository.save(log);

        // Redis Sorted Set 적재: score = 알람 예정 시각 epoch millis
        // 동일 member로 재호출 시 score만 갱신되므로 멱등하다.
        long score = scheduledDateTime
            .atZone(ZoneId.of("Asia/Seoul"))
            .toInstant()
            .toEpochMilli();
        ringingAlarmRedisRepository.add(alarmId, memberId, score);
    }

    @Override
    public void markReminderSent(Set<Long> occurrenceIds) {
        if (occurrenceIds == null || occurrenceIds.isEmpty()) return;
        // 벌크 업데이트가 가능하면 JPQL update 사용 권장 (락/동시성 고려)
        alarmOccurrenceRepository.markReminderSentIn(occurrenceIds);
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

}
