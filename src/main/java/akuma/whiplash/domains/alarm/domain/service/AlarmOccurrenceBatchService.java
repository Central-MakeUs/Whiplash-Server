package akuma.whiplash.domains.alarm.domain.service;

import akuma.whiplash.domains.alarm.application.dto.etc.AlarmOccurrenceCreateBatchResult;
import akuma.whiplash.domains.alarm.application.mapper.AlarmMapper;
import akuma.whiplash.domains.alarm.domain.constant.AlarmStatus;
import akuma.whiplash.domains.alarm.domain.util.AlarmScheduleCalculator;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmEntity;
import akuma.whiplash.domains.alarm.persistence.entity.AlarmOccurrenceEntity;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmOccurrenceRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository;
import akuma.whiplash.domains.alarm.persistence.repository.AlarmRepository.AlarmOccurrenceBatchTarget;
import akuma.whiplash.global.util.date.TimeProvider;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmOccurrenceBatchService {

    private final AlarmRepository alarmRepository;
    private final AlarmOccurrenceRepository alarmOccurrenceRepository;
    private final TimeProvider timeProvider;

    /**
     * 오늘 울릴 알람 중 아직 alarm_occurrence가 없는 알람에 대해 이력을 생성합니다.
     * - 이미 생성된 알람은 건너뜀
     * - 중간에 실패한 알람은 오류만 로깅하고 전체 배치에는 영향 없음
     */
    @Transactional
    public AlarmOccurrenceCreateBatchResult createTodayAlarmOccurrences() {
        List<AlarmOccurrenceBatchTarget> activeAlarms = alarmRepository.findBatchTargetsByStatus(AlarmStatus.ACTIVE.name());

        log.info("[AlarmOccurrence Create Batch] 활성 알람 수: {}", activeAlarms.size());

        int createdCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        // 사용자 timeZone 기준 오늘 반복 요일인 알람에 대해서만 alarm_occurrence를 생성한다.
        for (AlarmOccurrenceBatchTarget target : activeAlarms) {
            Long alarmId = target.getAlarmId();
            ZoneId memberZone = AlarmScheduleCalculator.resolveZone(target.getTimeZone());
            LocalDate today = timeProvider.today(memberZone);
            DayOfWeek todayDayOfWeek = today.getDayOfWeek();

            if (!containsRepeatDay(target.getRepeatDays(), todayDayOfWeek)) {
                skippedCount++;
                continue;
            }

            if (alarmOccurrenceRepository.existsByAlarmIdAndDate(alarmId, today)) {
                skippedCount++;
                continue;
            }

            try {
                AlarmEntity alarmReference = alarmRepository.getReferenceById(alarmId);
                LocalDateTime scheduledAt = AlarmScheduleCalculator.toDefaultZoneLocalDateTime(
                    today,
                    target.getAlarmTime(),
                    memberZone
                );
                AlarmOccurrenceEntity occurrence = AlarmMapper.mapToAlarmOccurrenceForDate(
                    alarmReference,
                    target.getAlarmTime(),
                    today,
                    scheduledAt
                );
                alarmOccurrenceRepository.save(occurrence);

                createdCount++;

                log.info("[AlarmOccurrence Create Batch] 생성 완료: alarmId={}, date={}", alarmId, today);

            } catch (Exception e) {
                failedCount++;
                log.error("[AlarmOccurrence Create Batch] 생성 실패: alarmId={}, error={}", alarmId, e.getMessage());
            }
        }

        log.info("[AlarmOccurrence Create Batch] 완료 - 생성: {}, 건너뜀: {}, 실패: {}",
            createdCount, skippedCount, failedCount);

        return AlarmOccurrenceCreateBatchResult.builder()
            .createdCount(createdCount)
            .skippedCount(skippedCount)
            .failedCount(failedCount)
            .build();
    }

    private boolean containsRepeatDay(String repeatDays, DayOfWeek dayOfWeek) {
        return repeatDays != null && repeatDays.contains("\"" + dayOfWeek.name() + "\"");
    }
}
